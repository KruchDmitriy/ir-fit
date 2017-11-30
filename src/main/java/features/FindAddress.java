package features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data_preprocess.InvertIndex;
import data_preprocess.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class FindAddress {

    private static final String pathToGrade = "./src/main/resources/grade.txt";
    private static final String pathToDocument = "../../../documents_small/";
    private static final String pathToNameDocument =
            "../../../index_file.json";


    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>
            // grade name to List from (name_doc, freq_grade_in_document)
            gradeToMapFromPathsToFreq = new ConcurrentHashMap<>();

    private List<String> readingGradeByLine = new LinkedList<>();

    private ConcurrentHashMap<String, Pattern> compilePattern = new
            ConcurrentHashMap<>();


    private ArrayList nameDocument = new ArrayList<>();
    private ConcurrentHashMap<String, Integer> nameDocumentToIndex = new
            ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

//    private static final String matchAddressForKartaSporta =
//            "address(.+)<br\\s+/>";
//    "<p>Адрес:\K(.*)(?=</p>)" - dance line
//    "address\">\K(.*)(?=\<)"



    void saveGrade() {
        loadArrayWithNameFiles();
        loadAllGrade();
        initCompilePattern();
        initMapWithGrade();
        findGradeInAllFiles();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter
                    ("./src/main/resources/gradeFind.txt"))) {
            GSON.toJson(gradeToMapFromPathsToFreq, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadArrayWithNameFiles() {
        nameDocument = InvertIndex.getNameArray(pathToNameDocument);
        if (nameDocument == null){
            System.out.println("");
            return;
        }
        for (int idx = 0; idx < nameDocument.size(); idx++) {
             nameDocumentToIndex.put(nameDocument.get(idx).toString(), idx);
        }
    }

    private void loadAllGrade() {
        try {
            Path path = Paths.get(pathToGrade);
            Files.readAllLines(path).stream()
                    .flatMap(Pattern.compile(";")::splitAsStream)
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
        paths.forEach(this::saveGradeFromFile);
    }

    private void saveGradeFromFile(@NotNull Path pathToFile) {
        try {
            String textFile = readFileToString(pathToFile);

            compilePattern.forEach((grade, pattern) -> {

                Matcher matcher = pattern.matcher(textFile);
                final Integer freq = countMatches(matcher);

                gradeToMapFromPathsToFreq.computeIfPresent(grade,
                        (gradeStr, pathIntegerConcurrentHashMap) ->
                                addToGradeMap(pathToFile.toString(), freq,
                                        pathIntegerConcurrentHashMap)
                );

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ConcurrentHashMap<String, Integer>
    addToGradeMap(@NotNull String path, @NotNull Integer freq,
                  ConcurrentHashMap<String, Integer> map) {
        if (freq > 0) {
            map.put(path, freq);
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


    private String readFileToString(@NotNull Path path) throws
            IOException {
        return Files.lines(path).collect(Collectors.joining());

    }

}
