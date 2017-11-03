package data_preprocess;

import org.apache.log4j.PropertyConfigurator;

public class PreProcMain {
    public static void main(String[] args) {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

//        SimplePreprocessing.simplProcAllFilesInDir("../ir-fit-data/texts/", "../ir-fit-data/fuck/");

        Stemming stemming = new Stemming();

        stemming.runStemming();
        stemming.writeHistogramToFile("src/main/resources/scripts/data_histogram.txt");
//        InvertIndex invertIndex = new InvertIndex("src/main/resources/tmp/");
//        invertIndex.start();
    }

}
