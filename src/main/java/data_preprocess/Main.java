package data_preprocess;

import org.apache.log4j.PropertyConfigurator;

/**
 * Created by kate on 26.10.17.
 */
public class Main {
    public static void main(String[] args) {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);
        Stemming stemming = new Stemming();

//        stemming.runStemming();
//        stemming.writeHistogramToFile("src/main/resources/scripts/data_histogram.txt");
        InvertIndex invertIndex = new InvertIndex("src/main/resources/tmp/");
    }

}
