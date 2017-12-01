package search;

public class BM25Stars extends BM25 {
    private static final double lambda = 1.25;

    public BM25Stars(double numberOfDocuments, double averageDocumentLength) {
        super(numberOfDocuments, averageDocumentLength);
    }

    public double score(double tf, double docLength, double queryFrequency,
                        double documentNumber, double stars) {
        return score(tf, docLength, queryFrequency, documentNumber) + lambda * stars;
    }
}
