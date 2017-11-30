package search;


public class BM25 {

    /** The constant k_1.*/
    private double k_1 = 1.2d;

    /** The constant k_3.*/
    private double k_3 = 8d;

    /** The parameter b.*/
    private double b;

    private double numberOfDocuments;

    private double averageDocumentLength;

    public void setNumberOfDocuments(double numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public void setAverageDocumentLength(double averageDocumentLength) {
        this.averageDocumentLength = averageDocumentLength;
    }

    /** A default constructor.*/
    public BM25() {
        super();
        b = 0.75d;
    }

    /**
     * Returns the name of the model.
     * @return the name of the model
     */
    public final String getInfo() {
        return "BM25 >> b="+b +", k_1=" + k_1 +", k_3=" + k_3;
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
                              double documentFrequency) {

        double K = k_1 * ((1 - b) + ((b * docLength) / averageDocumentLength));
        double weight = ( ((k_1 + 1d) * tf) / (K + tf) );	//first part
        weight = weight * ( ((k_3 + 1) * queryFrequency) / (k_3 + queryFrequency) );	//second part

        // multiply the weight with idf
        double idf = weight * Math.log((numberOfDocuments - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
        return idf;
    }


    /**
     * Sets the b parameter to BM25 ranking formula
     * @param b the b parameter value to use.
     */
    public void setParameter(double b) {
        this.b = b;
    }


    /**
     * Returns the b parameter to the BM25 ranking formula as set by setParameter()
     */
    public double getParameter() {
        return this.b;
    }

}
