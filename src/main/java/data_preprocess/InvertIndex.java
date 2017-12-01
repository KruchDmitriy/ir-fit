package data_preprocess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import data_preprocess.utils.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InvertIndex {
    private static final Logger LOGGER = Logger.getLogger(InvertIndex.class);
    private static final String IR_FIT_DATA_INDEX = "../ir-fit-data/index1/";

    private static final String FILE_INDEX_NAME = "files.json";
    private static final String WORD_FREQ_INDEX_NAME = "freqs.json";
    private static final String FILE_LENGTH_INDEX_NAME = "len_files.json";
    private static final String FILE_POSITIONS_INDEX_NAME = "file_pos.json";
    private static final String META_INDEX_NAME = "meta.json";

    private static final String PATH_INDEX_FILES = IR_FIT_DATA_INDEX + FILE_INDEX_NAME;
    private static final String PATH_INDEX_FREQS = IR_FIT_DATA_INDEX + WORD_FREQ_INDEX_NAME;
    private static final String PATH_INDEX_FILE_LENGTH = IR_FIT_DATA_INDEX + FILE_LENGTH_INDEX_NAME;
    private static final String PATH_INDEX_FILE_POSITIONS = IR_FIT_DATA_INDEX + FILE_POSITIONS_INDEX_NAME;
    private static final String PATH_INDEX_META = IR_FIT_DATA_INDEX + META_INDEX_NAME;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_FILES_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type FILES_LENGTH_TYPE = new TypeToken<Map<Integer, Integer>>() {}.getType();
    private static final Type WORD_TO_FREQ_FILE_TYPE = new TypeToken<SortedMap<String, LookUpTableFreq>>() {}.getType();

    private Map<String, Integer> fileIndex = new ConcurrentHashMap<>();
    private List<String> listFiles = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, Integer> fileLength = new ConcurrentHashMap<>();
    private SortedMap<String, LookUpTable> wordToPosInFile = Collections.synchronizedSortedMap(new TreeMap<>());
    private SortedMap<String, LookUpTableFreq> wordToFreqFile = Collections.synchronizedSortedMap(new TreeMap<>());
    private Meta meta;

    public static InvertIndex create(String pathToStemmedTexts, boolean createPositionIndex) throws IOException {
        InvertIndex invertIndex = new InvertIndex();
        LOGGER.info("getting unique words");
        invertIndex.createMapWithWords();

        LOGGER.info("creating index: word to freq in each file");
        invertIndex.createWordToFreqIndex(Paths.get(pathToStemmedTexts));
        invertIndex.dumpWordToFreqIndex();

        if (createPositionIndex) {
            LOGGER.info("creating index; word to position in each files");
            invertIndex.createWordToPosIndex(Paths.get(pathToStemmedTexts));
            invertIndex.dumpWordToPosIndex();
        }

        LOGGER.info("creating index; meta and file length");
        invertIndex.createMeta();
        invertIndex.dumpFileLength();
        invertIndex.dumpMeta();

        return invertIndex;
    }

    public static InvertIndex readFromDirectory() throws IOException {
        return readFromDirectory(null);
    }

    public static InvertIndex readFromDirectory(@Nullable String indexDirectory) throws IOException {
        InvertIndex invertIndex = new InvertIndex();

        if (indexDirectory == null) {
            indexDirectory = IR_FIT_DATA_INDEX;
        }

        invertIndex.listFiles = readFileIndex(indexDirectory + FILE_INDEX_NAME);
        invertIndex.fileIndex = new HashMap<>();
        for (int i = 0; i < invertIndex.listFiles.size(); i++) {
            invertIndex.fileIndex.put(invertIndex.listFiles.get(i), i);
        }

        invertIndex.wordToFreqFile = readWordToFreqIndex(indexDirectory + WORD_FREQ_INDEX_NAME);
        invertIndex.fileLength = readFileLengthIndex(indexDirectory + FILE_LENGTH_INDEX_NAME);
        invertIndex.meta = readMeta(indexDirectory + META_INDEX_NAME);

        return invertIndex;
    }

    public Set<Integer> getAllDocumentsIds() {
        return IntStream.rangeClosed(0, listFiles.size() - 1)
                .boxed()
                .collect(Collectors.toSet());
    }

    public double termFrequency(@NotNull String term, int documentId) {
        final LookUpTableFreq tableFreq = wordToFreqFile.get(term);
        return tableFreq.getFreq(documentId) / (double) fileLength.get(documentId);
    }

    public int documentsNumber(@NotNull String term) {
        final LookUpTableFreq tableFreq = wordToFreqFile.get(term);
        return tableFreq.size();
    }

    public int documentLength(int documentId) {
        return fileLength.get(documentId);
    }

    public String documentById(int documentId) {
        return listFiles.get(documentId);
    }

    public List<String> getDocumentsContainTerm(@NotNull String term) {
        final LookUpTableFreq tableFreq = wordToFreqFile.get(term);
        return tableFreq.getAllFiles()
                .stream()
                .map(fileId -> listFiles.get(fileId))
                .collect(Collectors.toList());
    }

    public List<Integer> getDocumentsIdContainTerm(@NotNull String term) {
        final LookUpTableFreq tableFreq = wordToFreqFile.get(term);
        return tableFreq == null ? Collections.EMPTY_LIST : new ArrayList<>(tableFreq.getAllFiles());
    }

    public Meta getMeta() {
        return meta;
    }

    public static List<String> readFileIndex(@NotNull String pathToIndexFile) throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(pathToIndexFile))) {
            return GSON.fromJson(reader, LIST_FILES_TYPE);
        }
    }

    public static SortedMap<String, LookUpTableFreq> readWordToFreqIndex(
            @NotNull String pathToIndexFile) throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(pathToIndexFile))) {
            return GSON.fromJson(reader, WORD_TO_FREQ_FILE_TYPE);
        }
    }

    public static Map<Integer, Integer> readFileLengthIndex(
            @NotNull String pathToIndexFile) throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(pathToIndexFile))) {
            return GSON.fromJson(reader, FILES_LENGTH_TYPE);
        }
    }

    public static Meta readMeta(@NotNull String pathToMeta) throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(pathToMeta))) {
            return GSON.fromJson(reader, Meta.class);
        }
    }

    public static class Meta {
        public final double numberOfDocuments;
        public final double averageDocumentLength;

        public Meta(double numberOfDocuments, double averageDocumentLength) {
            this.numberOfDocuments = numberOfDocuments;
            this.averageDocumentLength = averageDocumentLength;
        }
    }

    private void createWordToPosIndex(Path pathToStemmedTexts) throws IOException {
        List<Path> allFiles = Utils.getAllFiles(pathToStemmedTexts);
        List<Integer> listPosition;
        for (Path path: allFiles) {
            try {
                String content = new String(Files.readAllBytes(path));
                for (String word : wordToPosInFile.keySet()) {
                    listPosition = findStartIndexesForKeyword(word, content);
                        wordToPosInFile.get(word).addListPositionInFile(
                                fileIndex.get(path.getFileName().toString()), listPosition);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Integer> findStartIndexesForKeyword(String keyword, String searchString) {
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

    private void createWordToFreqIndex(Path pathToStemmedTexts) throws IOException {
        List<Path> files = Utils.getAllFiles(pathToStemmedTexts);
        if (files.isEmpty()) {
            LOGGER.warn("List of all files is empty");
        }

        AtomicInteger[] fileCounter = { new AtomicInteger() };

        files.stream().parallel()
            .peek(file -> {
                String fileName = file.getFileName().toString();
                if (!fileIndex.containsKey(fileName)) {
                    fileIndex.put(fileName, fileCounter[0].getAndIncrement());
                    listFiles.add(fileName);
                }
            })
            .forEach(file -> {
                Stream<String> wordInFile = null;
                try {
                    wordInFile = Utils.readWordsFromFile(file);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                final int[] wordsCounter = { 0 };
                final int fileId = fileIndex.get(file.getFileName().toString());

                assert wordInFile != null;
                wordInFile = wordInFile.peek(s -> wordsCounter[0]++);

                Map<String, Long> fileFreqMap = Utils.createFreqMap(wordInFile);
                fileLength.put(fileId, wordsCounter[0]);

                fileFreqMap.forEach(
                        (word, freq) -> wordToFreqFile.get(word)
                                .addFreq(fileId, (int) ((long) freq))
                );
            }
        );
    }

    private void createMeta() {
        double[] sumDocumentLength = {0.};
        fileLength.forEach((fileId, length) -> sumDocumentLength[0] += length);
        double documentCount = fileLength.size();
        meta = new Meta(documentCount, sumDocumentLength[0] /= documentCount);
    }

    private void dumpWordToFreqIndex() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FREQS))) {
            GSON.toJson(wordToFreqFile, writer);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FILES))) {
            GSON.toJson(listFiles, writer);
        }
    }

    private void dumpFileLength() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FILE_LENGTH))) {
            GSON.toJson(fileLength, writer);
        }
    }

    private void dumpMeta() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_META))) {
            GSON.toJson(meta, writer);
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

    private void dumpWordToPosIndex() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_INDEX_FILE_POSITIONS))) {
            GSON.toJson(wordToPosInFile, writer);
        }
    }
}
