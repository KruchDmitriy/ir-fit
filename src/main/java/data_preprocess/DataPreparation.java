package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataPreparation {
    public static void main(String[] args) throws IOException {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        if (!Files.exists(Paths.get(Utils.PATH_TO_PREPROCESSED_TEXTS))) {
            SimplePreprocessing.simpleProcAllFilesInDir(
                    Utils.PATH_TO_TEXTS,
                    Utils.PATH_TO_PREPROCESSED_TEXTS);
        }

        Stemming stemming = new Stemming();
        Path stemmingDir = Paths.get(Utils.PATH_TO_STEMMED_TEXTS);
        if (!Files.exists(stemmingDir)) {
            Files.createDirectory(stemmingDir);
            stemming.runStemming();
            stemming.writeHistogramToFile(Utils.PATH_TO_HIST);
            stemming.readHist(Utils.PATH_TO_HIST);
            stemming.runStemmingByFile();
        }

        InvertIndex invertIndex = new InvertIndex(Utils.PATH_TO_STEMMED_TEXTS);
        invertIndex.build();
    }
}
