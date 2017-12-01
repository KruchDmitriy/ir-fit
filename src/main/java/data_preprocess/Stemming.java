package data_preprocess;

import data_preprocess.utils.Utils;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stemming {
    private static final Logger LOGGER = Logger.getLogger(Stemming.class);
    private Map<String, Long> strToFrequency = new HashMap<>();
    private static final SnowballStemmer STEMMER = new SnowballStemmer(SnowballStemmer.ALGORITHM.RUSSIAN);
    private static final Set<String> STOP_WORDS = new HashSet<>();
    private static final String FILE_STOP_WORDS = "./src/main/resources/stop_words.txt";

    static {
        try {
            Files.lines(Paths.get(FILE_STOP_WORDS)).forEach(STOP_WORDS::add);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Stream<String> processWords(@NotNull Stream<String> words) {
        return words
            .filter(word -> !STOP_WORDS.contains(word))
            .map(STEMMER::stem)
            .map(CharSequence::toString);
    }

    public void createWordFrequencyMap(@NotNull String pathToPreprocessedTexts) {
        try {
            Stream<String> stringStream = Utils.readWords(Paths.get(pathToPreprocessedTexts));

            strToFrequency = stringStream
                    .map(STEMMER::stem)
                    .map(CharSequence::toString)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        } catch (IOException e) {
            LOGGER.debug(e.getMessage());
        }
    }

    public void runStemmingOnFiles(@NotNull String pathToPreprocessedTexts) {
        Utils.getAllFiles(Paths.get(pathToPreprocessedTexts)).forEach(path -> {
            String fileName = Utils.getLast(Arrays.asList(path.toString().split("/")));
            try (PrintWriter writer = new PrintWriter(Utils.PATH_TO_STEMMED_TEXTS + fileName)){
                processWords(Utils.readWordsFromFile(path))
                        .forEach(word -> writer.append(word).append(" "));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public Map<String, Long> readHist(final @NotNull String fileName) {
        try {
            Files.readAllLines(Paths.get(fileName)).forEach(line -> {
                String[] arr = line.split(" ");
                assert(arr.length == 2);
                strToFrequency.put(arr[0], Long.parseLong(arr[1]));
            } );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return strToFrequency;
    }

    public void writeHistogramToFile(final @NotNull String fileName) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(strToFrequency.entrySet());
        list.sort(Comparator.comparing(o -> -(o.getValue())));
        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (Map.Entry<String, Long> item : list) {
                writer.println(item.getKey() + " " + item.getValue());
            }
        } catch (IOException e) {
            LOGGER.debug(e.getMessage());
        }
    }
}
