package search;

import data_preprocess.InvertIndex;
import data_preprocess.Stemming;
import data_preprocess.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Search {
    private final InvertIndex invertIndex;
    private final BM25 bm25;

    public Search() throws IOException {
        invertIndex = InvertIndex.readFromDirectory();
        InvertIndex.Meta meta = invertIndex.getMeta();
        bm25 = new BM25(meta.numberOfDocuments, meta.averageDocumentLength);
    }

    public List<Document> process(@NotNull String query) {
        Map<String, Long> queryFreqMap = Utils.createFreqMap(Stemming.processWords(Utils.splitToWords(query)));

        List<Integer> documents = new ArrayList<>();
        queryFreqMap.forEach((term, queryFrequency) ->
                documents.addAll(invertIndex.getDocumentsIdContainTerm(term)));

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

        documents.sort(Comparator.comparingDouble(doc -> scores[doc]));
        return documents.stream()
                .map(invertIndex::documentById)
                .map(Document::new)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        final Search search = new Search();
        final Scanner scanner = new Scanner(System.in);

        while (true) {
            String query = scanner.next();
            if (query.equals("exit")) {
                return;
            }
            List<Document> documents = search.process(query);
            System.out.println("\n");
            System.out.println(documents.get(0));
            System.out.println("\n");
            System.out.println(documents.get(1));
            System.out.println("\n");
        }
    }
}
