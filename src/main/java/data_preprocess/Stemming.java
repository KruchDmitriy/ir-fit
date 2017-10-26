package data_preprocess;


import data_preprocess.utils.Utils;
import opennlp.tools.stemmer.PorterStemmer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by kate on 26.10.17.
 */
public class Stemming {

    private static final Logger LOGGER = Logger.getLogger(Stemming.class);

    private Map<String, Long> strToFrequency;

    private Stream<String> createTableWithFrequency(final @NotNull String dir) throws IOException {
        List<Path> allFiles = Utils.getAllFiles(dir);

        return allFiles.stream().map(path -> {
            try {
                return Files.lines(path);
            } catch (IOException e) {
                LOGGER.debug(String.format(" file: %s message: %s ",
                        path.getFileName(), e.getMessage()));
                return Stream.of("");
            }
        }).parallel().map(stringStream ->
                stringStream
                        .map(line -> line.split(" "))
                        .flatMap(Arrays::stream)).flatMap(Function.identity());

    }


    public void runStemming(final @NotNull String dir) {
        PorterStemmer stemmer = new PorterStemmer();
        try {
            Stream<String> stringStream = createTableWithFrequency(dir);
            strToFrequency = stringStream.parallel()
                    .map(stemmer::stem)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage());
        }
    }

    public void writeHistogramToFile(final @NotNull String fileName) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(strToFrequency.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (Map.Entry<String, Long> item : list) {
                writer.println(item.getKey() + " " + item.getValue());
            }
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage());
        }
    }
}
