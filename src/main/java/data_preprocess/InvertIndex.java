package data_preprocess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
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
    private static final String IR_FIT_DATA_INDEX = "../ir-fit-data/index/";

    private static final String FILE_INDEX_NAME = "files.json";
    private static final String WORD_FREQ_INDEX_NAME = "freqs.json";
    private static final String FILE_LENGTH_INDEX_NAME = "len_files.json";
    private static final String FILE_POSITIONS_INDEX_NAME = "index_file_pos.json";
    private static final String META_INDEX_NAME = "meta.json";
    private static final String TITLE_INDEX_NAME = "title.json";

    private static final String PATH_INDEX_FILES = IR_FIT_DATA_INDEX + FILE_INDEX_NAME;
    private static final String PATH_INDEX_FREQS = IR_FIT_DATA_INDEX + WORD_FREQ_INDEX_NAME;
    private static final String PATH_INDEX_FILE_LENGTH = IR_FIT_DATA_INDEX + FILE_LENGTH_INDEX_NAME;
    private static final String PATH_INDEX_FILE_POSITIONS = IR_FIT_DATA_INDEX + FILE_POSITIONS_INDEX_NAME;
    private static final String PATH_INDEX_META = IR_FIT_DATA_INDEX + META_INDEX_NAME;
    private static final String PATH_TITLE = IR_FIT_DATA_INDEX +
            TITLE_INDEX_NAME ;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_FILES_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type LIST_INT_TYPE = new TypeToken<List<Integer>>() {}.getType();
    private static final Type FILES_LENGTH_TYPE = new TypeToken<Map<Integer, Integer>>() {}.getType();
    private static final Type WORD_TO_FREQ_FILE_TYPE = new TypeToken<SortedMap<String, LookUpTableFreq>>() {}.getType();

    private static final Type TITLE_TYPE = new TypeToken<Map<Integer, String>>
            () { }.getType();

    private Map<String, Integer> fileIndex = new ConcurrentHashMap<>();
    private List<String> listFiles = Collections.synchronizedList(new ArrayList<>());
    private Map<Integer, Integer> fileLength = new ConcurrentHashMap<>();
    private SortedMap<String, LookUpTable> wordToPosInFile = Collections.synchronizedSortedMap(new TreeMap<>());
    private SortedMap<String, LookUpTableFreq> wordToFreqFile = Collections.synchronizedSortedMap(new TreeMap<>());

    private static Map<Integer, String> title = new HashMap<>();
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
            invertIndex.createAndDumpWordToPosIndex(Paths.get(pathToStemmedTexts));
        }

        LOGGER.info("creating index; meta and file length");
        invertIndex.createMeta();
        invertIndex.dumpFileLength();
        invertIndex.dumpMeta();

        return invertIndex;
    }

    private static void readTitle() throws IOException {

        try (Reader reader = new BufferedReader(new FileReader(PATH_TITLE))) {
            title = GSON.fromJson(reader, TITLE_TYPE);
        }
    }

    public Map<Integer, String> getTitle( ) {
        return title;
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

        readTitle();
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

    private void createAndDumpWordToPosIndex(Path pathToStemmedTexts) throws IOException {
        List<Path> allFiles = Utils.getAllFiles(pathToStemmedTexts);
        String[] fileContent = new String[allFiles.size()];

        for (Path path : allFiles) {
            final int fileId = fileIndex.get(path.getFileName().toString());
            fileContent[fileId] = new String(Files.readAllBytes(path));
        }

        final JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                new FileOutputStream(PATH_INDEX_FILE_POSITIONS), "UTF-8"));
//        writer.setIndent("  ");

//        PATH_INDEX_FILE_POSITIONS

        wordToPosInFile.keySet().stream()
                .forEach(word -> {
                    LOGGER.info("Processing word " + word);
                    final String regex = "\\b" + word + "\\b";
                    final Pattern pattern = Pattern.compile(regex);

                    try {
                        writer.beginObject();
                        writer.name(word);
                        writer.beginArray();

                        for (Integer fileId: wordToFreqFile.get(word).getAllFiles()) {
                            final List<Integer> listPosition = findStartIndexesForKeyword(fileContent[fileId], pattern);
                            GSON.toJson(listPosition, LIST_INT_TYPE, writer);
                            wordToPosInFile.get(word).addListPositionInFile(fileId, listPosition);
                        }

                        writer.endArray();
                        writer.endObject();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                );
//        for (String word: wordToPosInFile.keySet()) {
//
//        }

//        for (Path path: allFiles) {

//            try {
//                String content = new String(Files.readAllBytes(path));
//                for (String word : wordToPosInFile.keySet()) {
//                    listPosition = findStartIndexesForKeyword(word, content);
//                        wordToPosInFile.get(word).addListPositionInFile(
//                                fileIndex.get(path.getFileName().toString()), listPosition);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        writer.endArray();
        writer.close();
    }

    private static List<Integer> findStartIndexesForKeyword(String searchString, Pattern pattern) {
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

        listFiles.sort(Comparator.naturalOrder());
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
}
