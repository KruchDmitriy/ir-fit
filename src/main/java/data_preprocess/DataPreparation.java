package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataPreparation {
    private static final Logger LOGGER = Logger.getLogger(DataPreparation.class);

    public static void main(String[] args) throws IOException {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        LOGGER.info("Preprocessing files");
        if (!Files.exists(Paths.get(Utils.PATH_TO_PREPROCESSED_TEXTS))) {
            SimplePreprocessing.simpleProcAllFilesInDir(
                    Utils.PATH_TO_TEXTS,
                    Utils.PATH_TO_PREPROCESSED_TEXTS);
        }

        LOGGER.info("Run stemming on texts");
        Stemming stemming = new Stemming();
        Path stemmingDir = Paths.get(Utils.PATH_TO_STEMMED_TEXTS);
        if (!Files.exists(stemmingDir)) {
            LOGGER.info("Creating stemming directory");
            Files.createDirectory(stemmingDir);

            if (!Files.exists(Paths.get(Utils.PATH_TO_HIST))) {
                LOGGER.info("Running stemming");
                stemming.runStemming(Utils.PATH_TO_PREPROCESSED_TEXTS);
                LOGGER.info("Saving words histogram");
                stemming.writeHistogramToFile(Utils.PATH_TO_HIST);
            } else {
                LOGGER.info("Reading words histogram from file");
                stemming.readHist(Utils.PATH_TO_HIST);
            }
            LOGGER.info("Saving stemming to files");
            stemming.runStemmingByFile(Utils.PATH_TO_PREPROCESSED_TEXTS);
        }

        LOGGER.info("Creating index");
        InvertIndex.create(Utils.PATH_TO_STEMMED_TEXTS, false);
    }
}
