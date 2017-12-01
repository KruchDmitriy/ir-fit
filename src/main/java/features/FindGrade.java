package features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data_preprocess.InvertIndex;
import data_preprocess.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FindGrade {

    private static final String pathToGrade = "./src/main/resources/grade.txt";
    private static final String pathToDocument = "../../../documents/";

    private static final String outFileWithGrade = "" +
            "./src/main/resources/gradeFind.txt";


    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>
            // grade name to List from (id_name_doc, freq_grade_in_document)
            gradeToMapFromPathsToFreq = new ConcurrentHashMap<>();

    private List<String> readingGradeByLine = new LinkedList<>();

    private ConcurrentHashMap<String, Pattern> compilePattern = new
            ConcurrentHashMap<>();


    private ConcurrentHashMap<String, Integer> nameDocumentToIndex = new
            ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();



    void saveGrade() throws IOException {
        features.utils.Utils.loadArrayWithNameFiles();
        nameDocumentToIndex = features.utils.Utils.getNameDocumentToIndex();
        loadAllGrade();
        initCompilePattern();
        initMapWithGrade();
        findGradeInAllFiles();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter
                (outFileWithGrade))) {
            GSON.toJson(gradeToMapFromPathsToFreq, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAllGrade() {
        try {
            Path path = Paths.get(pathToGrade);
            Files.readAllLines(path).stream()
                    .flatMap(Pattern.compile(";")::splitAsStream)
                    .map(String::toLowerCase)
                    .forEach(readingGradeByLine::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCompilePattern() {
        readingGradeByLine.forEach(grade ->
                compilePattern.put(grade, Pattern.compile(grade))
        );
    }

    private void initMapWithGrade() {
        readingGradeByLine.forEach(grade -> gradeToMapFromPathsToFreq.put(grade,
                new ConcurrentHashMap<>()));
    }

    private void findGradeInAllFiles() {
        List<Path> paths = Utils.getAllFiles(Paths.get(pathToDocument));
        paths.parallelStream().forEach(this::saveGradeFromFile);
    }

    private void saveGradeFromFile(@NotNull Path pathToFile) {
        try {
            String textFile = readFileToString(pathToFile);

            compilePattern.forEach((grade, pattern) -> {

                Matcher matcher = pattern.matcher(textFile);
                final Integer freq = countMatches(matcher);

                gradeToMapFromPathsToFreq.computeIfPresent(grade,
                        (gradeStr, pathIntegerConcurrentHashMap) ->
                                addToGradeMap(pathToFile, freq,
                                        pathIntegerConcurrentHashMap)
                );

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ConcurrentHashMap<Integer, Integer>
    addToGradeMap(@NotNull Path path, @NotNull Integer freq,
                  ConcurrentHashMap<Integer, Integer> map) {
        if (freq > 0) {
            map.put(nameDocumentToIndex.get(path.getFileName().toString()), freq);
        }
        return map;
    }

    private static Integer countMatches(@NotNull final Matcher matcher) {
        Integer freq = 0;
        while (matcher.find()) {
            ++freq;
        }
        return freq;
    }


    public static String readFileToString(@NotNull Path path) throws
            IOException {

//        StringBuilder fileContents = new StringBuilder();
//        try (Reader reader = new FileReader(path.toString())) {
//
//            int i;
//
//            while ((i = reader.read()) != -1) {
//                fileContents.append((char) i);
//            }
////        return Files.lines(path).collect(Collectors.joining()).toLowerCase();
//
//        }

        StringBuilder sb = new StringBuilder();
        try (InputStream is = new FileInputStream(path.toString());
             BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
            String line = buf.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }
        }
        return sb.toString().toLowerCase();

    }
}