package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InvertIndex {

    private static final Logger LOGGER = Logger.getLogger(InvertIndex.class);
    private static final String OUTPUT_DIR = "src/main/resources/index/";

    private final Path pathToDir;
    private List<String> uniqueWords = new ArrayList<>();

    private Map<String, List<String>> wordToListFiles = new HashMap<>();
    private Map<String, LookUpTable> wordToListPositionInFile = new HashMap<>();
    private Map<String, LookUpTableFreq> wordToFreqWordInFile = new HashMap<>();

    InvertIndex(String pathToDirWithFiles) {
        pathToDir = Paths.get(pathToDirWithFiles);
    }

    void start() {
        LOGGER.debug(" get uniq words");
        createMapWithWords();
        LOGGER.debug(" create index; word to file");
        createWordsToFiles();
        LOGGER.debug(" create index; word to position in each files");
        createWordToPositionInFiles();
        LOGGER.debug("create index; word to freq in each file");
        createWordsToFreqInFiles();
    }

    private void createWordToPositionInFiles() {
        List<Path> allFiles = Utils.getAllFiles(pathToDir);

        wordToListPositionInFile.entrySet().stream().parallel()
                .forEach(entry -> entry
                        .getValue()
                        .initFileInMap(allFiles));


        allFiles.stream().parallel().forEach(path -> {
                    try {
                        String textFile = new String(Files.readAllBytes(path));

                        for (String word : wordToListPositionInFile.keySet()) {
                            wordToListPositionInFile.get(word)
                                    .addListPositionInFile(path.toString(),
                                            findStartIndexesForKeyword(word, textFile));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        dumpToFileWordToPositionInFiles(OUTPUT_DIR);

    }

    private void createWordsToFreqInFiles() {
        List<Path> files = Utils.getAllFiles(pathToDir);
        files.stream().parallel().forEach(file -> {
                    try {
                        Stream<String> wordInFile = Utils.readFile(file);

                        Map<String, Long> fileFreqMap = Utils.createFeqMap(wordInFile);

                        fileFreqMap.forEach(
                                (word, freq) -> wordToFreqWordInFile.
                                        get(word)
                                        .addFreq(file.toString(), freq)
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        dumpToFileWordToFreqInFiles(OUTPUT_DIR);
    }

    private void dumpToFileWordToFreqInFiles(String pathToDir) {
        List<Map.Entry<String, LookUpTableFreq>> list = sortMapByKey(wordToFreqWordInFile);
        List<String> lines = new ArrayList<>();
        list.forEach(stringLookUpTableEntry -> lines.add(stringLookUpTableEntry.getKey() +
                " " + stringLookUpTableEntry.getValue().toString())
        );
        pathToDir += "idx_freq_";
        runDump(pathToDir, lines);
    }

    private void createWordsToFiles() {

        List<Path> allFiles = Utils.getAllFiles(pathToDir);

        for (Path path : allFiles) {
            List<String> wordInFile = null;

            try {
                wordInFile = Utils.readWords(path).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (wordInFile != null) {
                wordInFile.stream().parallel()
                        .distinct()
                        .forEach(word -> wordToListFiles
                                .get(word)
                                .add(path.toString()));
            }
        }

        dumpToFileWordToListFiles(OUTPUT_DIR);
    }

    private void createMapWithWords() {

        Stream<String> wordStream = null;
        try {
            wordStream = Utils.readWords(pathToDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (wordStream != null) {
            uniqueWords = wordStream.parallel()
                    .distinct()
                    .collect(Collectors.toList());
        }

        uniqueWords
                .forEach(word -> {
                    wordToListFiles.put(word, new ArrayList<>());
                    wordToListPositionInFile.put(word, new LookUpTable());
                    wordToFreqWordInFile.put(word, new LookUpTableFreq());
                });
    }


    private void dumpToFileWordToListFiles(String pathToDir) {
        List<Map.Entry<String, List<String>>> listEntryMap = sortMapByKey(wordToListFiles);
        List<String> lines = new ArrayList<>();
        listEntryMap.forEach(stringListEntry -> lines.add(lineBuilder(stringListEntry.getKey(),
                stringListEntry.getValue())));
        pathToDir += "idx_files_";
        runDump(pathToDir, lines);

    }

    private static void dumpToFile(BiFunction<PrintWriter, String, Void> function, String pathToFile,
                                   List<String> lines) {
        try (PrintWriter writerAE = new PrintWriter(pathToFile + "а-ё.txt");
             PrintWriter writerЖМ = new PrintWriter(pathToFile + "ж-м.txt");
             PrintWriter writerНУ = new PrintWriter(pathToFile + "н-у.txt");
             PrintWriter writerФЩ = new PrintWriter(pathToFile + "ф_щ.txt");
             PrintWriter writerЪЯ = new PrintWriter(pathToFile + "ъ_я.txt")) {
            lines.forEach(line -> {
                        switch (line.charAt(0)) {
                            case 'а':
                            case 'б':
                            case 'в':
                            case 'г':
                            case 'д':
                            case 'е':
                            case 'ё':
                                function.apply(writerAE, line);
                                break;
                            case 'ж':
                            case 'з':
                            case 'и':
                            case 'к':
                            case 'л':
                            case 'м':
                                function.apply(writerЖМ, line);
                                break;
                            case 'н':
                            case 'о':
                            case 'п':
                            case 'р':
                            case 'с':
                            case 'т':
                            case 'у':
                                function.apply(writerНУ, line);
                                break;
                            case 'ф':
                            case 'х':
                            case 'ц':
                            case 'ч':
                            case 'ш':
                            case 'щ':
                                function.apply(writerФЩ, line);
                                break;
                            case 'ъ':
                            case 'ы':
                            case 'ь':
                            case 'э':
                            case 'ю':
                            case 'я':
                                function.apply(writerЪЯ, line);
                                break;
                        }
                    }
            );
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void runDump(String path, List<String> lines) {
        dumpToFile((writer, s) -> {
                    writeToFile(writer, s);
                    return null;
                },
                path, lines);
    }

    private void dumpToFileWordToPositionInFiles(String pathToDir) {
        List<Map.Entry<String, LookUpTable>> list = sortMapByKey(wordToListPositionInFile);
        List<String> lines = new ArrayList<>();
        list.stream().parallel()
                .forEach(stringLookUpTableEntry ->
                        lines.add(stringLookUpTableEntry.getKey() +
                                " " + stringLookUpTableEntry.getValue().toString())
                );
        pathToDir += "idx_pos_";
        runDump(pathToDir, lines);
    }

    private static String lineBuilder(String word, List<String> stringList) {
        StringBuilder builder = new StringBuilder(word)
                .append(" ");
        stringList.forEach(s -> builder
                .append(s)
                .append(";"));
        return builder.toString();
    }

    private static void writeToFile(PrintWriter writer, String line) {
        writer.write(line + "\n");
    }

    private static List<Integer> findStartIndexesForKeyword(String keyword,
                                                            String searchString) {
        String regex = "\\b" + keyword + "\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(searchString);

        List<Integer> wrappers = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            wrappers.add(start);
        }
        return wrappers;
    }

    private static <K extends String, V> List<Map.Entry<K, V>> sortMapByKey(Map<K, V> kvMap) {
        List<Map.Entry<K, V>> listEntryMap = new ArrayList<>(kvMap.entrySet());
        listEntryMap.sort(Comparator.comparing(stringListEntry -> (stringListEntry.getKey())));
        return listEntryMap;
    }
}
