package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stemming {

    private static final Logger LOGGER = Logger.getLogger(Stemming.class);
    private Map<String, Long> strToFrequency;
    private String str = "мамой вылось рамы";

    private void createFrequencyTable(final @NotNull String dir) throws IOException {
        List<Path> allFiles = Utils.getAllFiles(Paths.get(dir));

        strToFrequency = allFiles.stream().map(path -> {
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
                        .flatMap(Arrays::stream)).flatMap(Function.identity())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

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
