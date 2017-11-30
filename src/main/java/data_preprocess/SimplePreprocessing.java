package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SimplePreprocessing {
    private static void fileToLowerCase(final @NotNull Path fileName, final @NotNull String outFile) {
        try (Stream<String> stream = Files.lines(fileName);
             PrintWriter writer = new PrintWriter(outFile)) {
            stream.parallel()
                    .map(String::toLowerCase)
                    .map(SimplePreprocessing::processWord)
                    .forEach(writer::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void simpleProcAllFilesInDir(String directory, String outDirectory) {
        List<Path> paths = Utils.getAllFiles(Paths.get(directory));
        paths.stream().parallel()
                .forEach(path -> {
                    String fileName = Utils.getLast(Arrays.asList(path.toString().split("/")));
                    fileToLowerCase(path, outDirectory + fileName);
                }
        );
    }

    private static String processWord(final @NotNull String str) {
        return str.replaceAll("\\p{Punct}+", " ");
    }

}

