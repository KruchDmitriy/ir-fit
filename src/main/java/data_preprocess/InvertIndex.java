package data_preprocess;

import data_preprocess.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
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

/**
 * Created by kate on 31.10.17.
 */

public class InvertIndex {

    private static final Logger LOGGER = Logger.getLogger(InvertIndex.class);
    private static final String OUTPUT_DIR = "src/main/resources/index/";

    private final Path pathToDir;
    private List<String> uniqueWords = new ArrayList<>();
    private Map<String, List<String>> wordToListFiles = new HashMap<>();
    private Map<String, LookUpTable> wordToListPositionInFile = new HashMap<>();

    public InvertIndex(String pathToDirWithFiles) {
        pathToDir = Paths.get(pathToDirWithFiles);
    }

    public void start() {
        createMapWithWords();
        createWordsToFiles();
        createWordToPositionInFiles();
    }

    public List<String> getUniqueWords() {
        return uniqueWords;
    }

    private void createWordToPositionInFiles() {
        List<Path> allFiles = Utils.getAllFiles(pathToDir);
        List<Integer> listPosition;
        for (Path path : allFiles) {
            try {
                String content = new String(Files.readAllBytes(path));
                for (String word : wordToListPositionInFile.keySet()) {
                    listPosition = findStartIndexesForKeyword(word, content);
                    wordToListPositionInFile.get(word).addListPositionInFile(path.toString(),
                            listPosition);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dumpToFileWordToPositionInFiles(OUTPUT_DIR);

    }


    public void createWordsToFreqInFiles() {

    }

    private void createWordsToFiles() {

        List<Path> allFiles = Utils.getAllFiles(pathToDir);

        for (Path path : allFiles) {
            List<String> wordInFile = Utils.readFiles(path).collect(Collectors.toList());
            wordInFile.stream().distinct().forEach(word -> wordToListFiles
                    .get(word)
                    .add(path.toString()));
        }

        dumpToFileWordToListFiles(OUTPUT_DIR);
    }

    private void createMapWithWords() {
        Stream<String> wordStream = Utils.readFiles(pathToDir);
        uniqueWords = wordStream.distinct().collect(Collectors.toList());
        uniqueWords
                .forEach(word -> {
                    wordToListFiles.put(word, Collections.emptyList());
                    wordToListPositionInFile.put(word, new LookUpTable());
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
        list.forEach(stringLookUpTableEntry -> {
                    lines.add(stringLookUpTableEntry.getKey() +
                            " " + stringLookUpTableEntry.getValue().toString());

                }
        );
        pathToDir += "idx_pos";
        runDump(pathToDir, lines);
    }

    private static String lineBuilder(String word, List<String> stringList) {
        StringBuilder builder = new StringBuilder(word);
        stringList.forEach(s -> {
            builder
                    .append(s)
                    .append(";");
        });
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
        listEntryMap.sort(new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> stringListEntry,
                               Map.Entry<K, V> stringListEntry1) {
                return (stringListEntry.getKey()).compareTo(stringListEntry1.getKey());
            }
        });
        return listEntryMap;
    }
}
