package data_preprocess.utils;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class);
    public static final String PATH_TO_TEXTS = "../ir-fit-data/texts/";
    public static final String PATH_TO_PREPROCESSED_TEXTS = "../ir-fit-data/preprocessed_texts/";
    public static final String PATH_TO_STEMMED_TEXTS = "../ir-fit-data/stemmed_texts/";
    public static final String PATH_TO_HIST = "src/main/resources/scripts/data_histogram.txt";
    private static final String REGEX_SPLIT_TO_WORDS = "[^a-zA-ZА-Яа-я]+";

    public static List<Path> getAllFiles(final @NotNull Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException ex) {
            LOGGER.debug(String.format("In dir %s exception message: %s",
                    directory, ex.getMessage()));
            return Collections.emptyList();
        }
    }

    private static Stream<String> readFiles(Path directory) {
        Charset charset = Charset.defaultCharset();
        final CharsetEncoder charsetEncoder = charset.newEncoder();
        charsetEncoder.onMalformedInput(CodingErrorAction.REPLACE);
        charsetEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        return getAllFiles(directory).stream()
                .flatMap((Path file) -> {
                    try {
                        return Files.lines(file);
                    } catch (UncheckedIOException | IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull);
    }

    public static Stream<String> splitToWords(final @NotNull String string) {
        return Arrays.stream(string.split(REGEX_SPLIT_TO_WORDS));
    }

    public static Stream<String> readWords(final @NotNull Path dir) throws IOException {
        return Utils.readFiles(dir)
                .map(s -> s.split(REGEX_SPLIT_TO_WORDS))
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty());
    }

    public static Stream<String> readFile(final @NotNull Path file) throws IOException {
        return Files.lines(file)
                .map(s -> s.split(REGEX_SPLIT_TO_WORDS))
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty());
    }

    public static Map<String, Long> createFreqMap(Stream<String> streamOfWords) {
        return streamOfWords.parallel()
                .map(CharSequence::toString)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    public static <T> T getLast(List<T> list) {
        return list != null && !list.isEmpty() ? list.get(list.size() - 1) : null;
    }
}
