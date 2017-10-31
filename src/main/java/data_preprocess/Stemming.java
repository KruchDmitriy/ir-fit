package data_preprocess;

import data_preprocess.utils.Utils;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stemming {

    private static final Logger LOGGER = Logger.getLogger(Stemming.class);

    private Map<String, Long> strToFrequency;

    public Map<String, Long> getStrToFrequency() {
        return strToFrequency;
    }

    public void runStemming() {
        SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.RUSSIAN);
        try {
            Stream<String> stringStream = Utils.readWords("../ir-fit-data/texts/");
//                    .filter(s -> !s.matches("^\\s*([0-9]+)\\s*"));

            strToFrequency = stringStream
                    .map(stemmer::stem)
                    .map(CharSequence::toString)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage());

        }
    }

    public void writeHistogramToFile(final @NotNull String fileName) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(strToFrequency.entrySet());
        Collections.sort(list, Comparator.comparing(o -> -(o.getValue())));
        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (Map.Entry<String, Long> item : list) {
                writer.println(item.getKey() + " " + item.getValue());
            }
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage());
        }
    }
}
