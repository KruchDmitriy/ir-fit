package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
    private int treshold = 1500;

    private Map<String, Long> strToFrequency;

    private String str = " мамой вылось рамы";


    private void createTableWithFrequency(final @NotNull String dir) throws IOException {
        List<Path> allFiles = Utils.getAllFiles(dir);

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

    private void writeHistogramToFile(final @NotNull String fileName) {
//        strToFrequency.s;
    }


}
