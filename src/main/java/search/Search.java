package search;

import data_preprocess.InvertIndex;
import data_preprocess.Stemming;
import data_preprocess.utils.Utils;
import features.FindAddress;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Search {
    private final InvertIndex invertIndex;
    private final BM25 bm25;
    private final FindAddress address;


    public Search() throws IOException {
        invertIndex = InvertIndex.readFromDirectory();
        InvertIndex.Meta meta = invertIndex.getMeta();
        bm25 = new BM25(meta.numberOfDocuments, meta.averageDocumentLength);
        address = new FindAddress();
        address.loadAddressFromJson();

        LoadUrls.loadJsonFileWithIdxToUrlOriginAddress();
        GenerateStartForDocument.loadFromJsonCountStars();
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
        }

        final SortedMap<Double, Integer> ranging = new TreeMap<>((o1, o2) -> -Double.compare(o1, o2));
        for (int i = 0; i < scores.length; i++) {
            ranging.put(scores[i], documents.get(i));
        }

        System.out.println("min " + ranging.firstKey());
        System.out.println("max " + ranging.lastKey());

        return ranging.values().stream()
                .map(idx -> {
                    String url = LoadUrls.getUrlById(idx);
                    if (url == null) {
                        url = invertIndex.documentById(idx).replaceAll("_", "/");
                    }

                    return new Document(
                            url,
                            GenerateStartForDocument.getStarsById(idx),
                            address.getListByIdDoc(idx));
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
