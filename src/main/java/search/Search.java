package search;

import data_preprocess.InvertIndex;
import data_preprocess.Stemming;
import data_preprocess.utils.Utils;
import features.FindAddress;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Search {
    private final static double lambdaStars = 0.2;
    private final InvertIndex invertIndex;
    private final BM25 bm25;
    private final FindAddress address;

    public Search() {
        try {
            System.out.println("start load search");
            invertIndex = InvertIndex.readFromDirectory();

            InvertIndex.Meta meta = invertIndex.getMeta();
            bm25 = new BM25(meta.numberOfDocuments, meta.averageDocumentLength);
            address = new FindAddress();
            address.loadAddressFromJson();

            LoadUrls.loadJsonFileWithIdxToUrlOriginAddress();
            GenerateStarsForDocument.loadFromJsonCountStars();
            System.out.println("stop load search");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Document> process(@NotNull String query) {
        final Map<String, Long> queryFreqMap = Utils.createFreqMap(Stemming.processWords(Utils.splitToWords(query)));

        final Set<Integer> documentsSet = invertIndex.getAllDocumentsIds();
        queryFreqMap.forEach((term, queryFrequency) ->
                documentsSet.retainAll(invertIndex.getDocumentsIdContainTerm(term)));

        final List<Integer> documents = new ArrayList<>(documentsSet);

        final double[] scores = new double[documents.size()];
        for (int i = 0; i < documents.size(); i++) {
            final int finalDocId = documents.get(i);
            final int finalI = i;
            queryFreqMap.forEach((term, queryFrequency) -> {
                final double termFrequency = invertIndex.termFrequency(term, finalDocId);
                final double documentLength = invertIndex.documentLength(finalDocId);
                scores[finalI] += bm25.score(termFrequency, documentLength,
                        queryFrequency, documents.size());
            });

            scores[i] += lambdaStars * GenerateStarsForDocument.getStarsById(finalDocId);
        }

        final SortedMap<Double, Integer> ranging = new TreeMap<>((o1, o2) -> -Double.compare(o1, o2));
        for (int i = 0; i < scores.length; i++) {
            ranging.put(scores[i], documents.get(i));
        }

        return ranging.values().stream()
                .map(documentId -> {
                    String url = LoadUrls.getUrlById(documentId);
                    if (url == null) {
                        url = invertIndex.documentById(documentId).replaceAll("_", "/");
                    }
                    String text = "";
                    try {
                        text = Files
                                .lines(Paths.get(Utils.PATH_TO_TEXTS+ invertIndex.documentById(documentId)))
                                .flatMap(Pattern.compile("\\s+")::splitAsStream).limit(100)
                                .collect(Collectors.joining(" "));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    return new Document(
                            url,
                            GenerateStarsForDocument.getStarsById(documentId),
                            address.getListByIdDoc(documentId),
                            text);
                })
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        final Search search = new Search();
        final Scanner scanner = new Scanner(System.in);
        System.out.println("ready");

        while (true) {
            String query = scanner.nextLine();
            if (query.equals("exit")) {
                return;
            }
            final List<Document> documents = search.process(query);
            System.out.println("\n");
            for (int i = 0; i < Math.min(documents.size(), 10); i++) {
                System.out.println(documents.get(i));
            }
            System.out.println("\n");
        }
    }
}
