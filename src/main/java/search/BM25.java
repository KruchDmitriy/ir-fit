package search;


public class BM25 {
    private static final double k_1 = 1.2d;
    private static final double k_3 = 8d;
    private static final double b = 0.75d;
    private final double numberOfDocuments;
    private final double averageDocumentLength;

    public BM25(double numberOfDocuments, double averageDocumentLength) {
        this.numberOfDocuments = numberOfDocuments;
        this.averageDocumentLength = averageDocumentLength;
    }

    /**
     * Uses BM25 to compute a weight for a term in a document.
     * @param tf The term frequency in the document
     * @param docLength the document's length
     * @return the score assigned to a document with the given
     *         tf and docLength, and other preset parameters
     */
    public final double score(double tf,
                              double docLength,
                              double queryFrequency,
                              double documentNumber) {
        double K = k_1 * ((1 - b) + ((b * docLength) / averageDocumentLength));
        double weight = ( ((k_1 + 1d) * tf) / (K + tf) );
        weight = weight * ( ((k_3 + 1) * queryFrequency) / (k_3 + queryFrequency) );

        double idf = Math.log((numberOfDocuments - documentNumber + 0.5d) / (documentNumber + 0.5d));
        return weight * idf;
    }
}
