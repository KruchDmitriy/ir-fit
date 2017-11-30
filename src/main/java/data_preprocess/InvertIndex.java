package data_preprocess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class InvertIndex {
    private static final Logger LOGGER = Logger.getLogger(InvertIndex.class);
    private static final String IR_FIT_DATA_INDEX = "../ir-fit-data/index/";
    private static final String PATH_INDEX_FILES = IR_FIT_DATA_INDEX + "index_files.json";
    private static final String PATH_INDEX_FREQS = IR_FIT_DATA_INDEX + "index_freqs.json";
    private static final String PATH_INDEX_FILE_POSITIONS = IR_FIT_DATA_INDEX + "index_file_pos.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path pathToDir;

    private Map<String, Integer> fileIndex = new ConcurrentHashMap<>();
    private List<String> listFiles = Collections.synchronizedList(new ArrayList<>());
    private SortedMap<String, LookUpTable> wordToPosInFile = Collections.synchronizedSortedMap(new TreeMap<>());
    private SortedMap<String, LookUpTableFreq> wordToFreqFile = Collections.synchronizedSortedMap(new TreeMap<>());

    InvertIndex(String pathToDirWithFiles) {
        pathToDir = Paths.get(pathToDirWithFiles);
    }

    void build() throws IOException {
        LOGGER.debug("getting unique words");
        createMapWithWords();
//        LOGGER.debug("creating index; word to position in each files");
//        createWordToPositionInFiles();
        LOGGER.debug("creating index: word to freq in each file");
        createWordsToFreqInFiles();
    }

    private void createWordsToFreqInFiles() throws IOException {
        List<Path> files = Utils.getAllFiles(pathToDir);
        if (files.isEmpty()) {
            LOGGER.warn("List of all files is empty");
        }

        AtomicInteger[] counter = { new AtomicInteger() };

        files.stream().parallel()
            .peek(file -> {
                String fileName = file.toString();
                if (!fileIndex.containsKey(fileName)) {
                    fileIndex.put(fileName, counter[0].getAndIncrement());
                    listFiles.add(fileName);
                }
            })
            .forEach(file -> {
                Stream<String> wordInFile = Utils.readFile(file);

                Map<String, Long> fileFreqMap = Utils.createFreqMap(wordInFile);

                fileFreqMap.forEach(
                        (word, freq) -> wordToFreqFile.get(word)
                                .addFreq(fileIndex.get(file.toString()), freq)
                );
            }
        );

        dumpWordToFreqIndex();
    }

    private void dumpWordToFreqIndex() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FREQS))) {
            GSON.toJson(wordToFreqFile, writer);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FILES))) {
            GSON.toJson(listFiles, writer);
        }
    }

    private void createMapWithWords() {
        try {
            Files.readAllLines(Paths.get(Utils.PATH_TO_HIST)).forEach(line -> {
                String[] arr = line.split(" ");
                assert(arr.length == 2);
                wordToPosInFile.put(arr[0], new LookUpTable());
                wordToFreqFile.put(arr[0], new LookUpTableFreq());
            } );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpWordToPosIndex(String indexFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FILES))) {
            GSON.toJson(wordToPosInFile, writer);
        }
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
}
