package data_preprocess;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by kate on 26.10.17.
 */
public class SimplePreprocessing {
    private static final Logger LOGGER = Logger.getLogger(SimplePreprocessing.class);

    private final String directory;
    private final String outDirectory;

    public SimplePreprocessing(String directory, String outDirectory) {
        this.directory = directory;
        this.outDirectory = outDirectory;
    }

    public void fileToLowerCase(final @NotNull String fileName, final @NotNull String outFile) {
        try (Stream<String> stream = Files.lines(Paths.get(fileName));
             PrintWriter writer = new PrintWriter(outFile)) {
            stream.parallel()
                    .map(String::toLowerCase)
                    .map(SimplePreprocessing::processWord)
                    .forEach(writer::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processWord(final @NotNull String str) {
        return str.replaceAll("\\p{Punct}+", "");
    }

}

