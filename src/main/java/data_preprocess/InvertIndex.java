package data_preprocess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InvertIndex {
    private static final Logger LOGGER = Logger.getLogger(InvertIndex.class);
    private static final String IR_FIT_DATA_INDEX = "../ir-fit-data/index/";
    private static final String PATH_INDEX_FREQS = IR_FIT_DATA_INDEX + "index_freqs.json";
    private static final String PATH_INDEX_FILE_POSITIONS = IR_FIT_DATA_INDEX + "index_file_pos.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path pathToDir;
    private List<String> uniqueWords = new ArrayList<>();

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

        files.stream().parallel()
//                .map(Utils::readFile)
//                .map(Utils::createFreqMap)
//                .forEach(map -> map.forEach());

            .forEach(file -> {
                Stream<String> wordInFile = Utils.readFile(file);

                Map<String, Long> fileFreqMap = Utils.createFreqMap(wordInFile);

                fileFreqMap.forEach(
                        (word, freq) -> wordToFreqFile.get(word)
                                .addFreq(file.toString(), freq)
                );
            }
        );

        dumpWordToFreqIndex(PATH_INDEX_FREQS);
    }

    private void dumpWordToFreqIndex(String indexFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
        GSON.toJson(wordToFreqFile, writer);
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
        String json = GSON.toJson(wordToPosInFile);
        Files.write(Paths.get(indexFile), json.getBytes());
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
