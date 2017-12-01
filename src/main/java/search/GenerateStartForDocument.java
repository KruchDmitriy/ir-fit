package search;

import com.google.gson.reflect.TypeToken;
import features.FindGrade;
import features.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateStartForDocument {

    private static final String pathGradeJson =
            "./src/main/resources/gradeFind.json";

    private static final String pathCountStars =
            "./src/main/resources/startForFile.json";


    private static ConcurrentHashMap<Integer, Double>
            idxDocumentToStartValue = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Double> gradeToCountStars = new
            ConcurrentHashMap<>();


    private static final Type TYPE_MAP_WITH_COUNT_STARS = new
            TypeToken<ConcurrentHashMap<Integer, Double>>() {
            }.getType();


    private static final FindGrade grade = new FindGrade();

    public static void loadGradeFromJson() {
        grade.loadGradeFromJson();
        generateScoreForEachGrade();
    }


    private static void generateScoreForEachGrade() {
        try {
            gradeToCountStars = FindGrade.loadGradeFromFileWithStars();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Double calculateStartForDocument(
            @NotNull final Integer idxDocument) {

        List<Double> stars = new LinkedList<>();
        grade.getGradeNameToMapFromIdxDocToFreqGrade()
                .forEach((nameGrade, idxToFreq) -> {
                    if (idxToFreq.containsKey(idxDocument)) {
//                        System.out.println(nameGrade + " " +
//                                gradeToCountStars.containsKey(nameGrade
//                                        .toLowerCase())
//                        + " " + gradeToCountStars.get(nameGrade.toLowerCase())
//                         );
                        stars.add(gradeToCountStars.get(nameGrade.toLowerCase
                                ().replaceAll(" ", "")));
                    }
                });
        stars.forEach(s -> System.out.print(s + " "));
        System.out.println();
        Collections.sort(stars);
        return stars.isEmpty() ? .0 : stars.get(0);
    }

    public static void calculateStarsForAllDocument() {
        try {
            Utils.loadArrayWithNameFiles();
            Utils.getNameDocumentToIndex().forEach(
                    (namDoc, idxDoc) ->
                            idxDocumentToStartValue.put(idxDoc,
                                    calculateStartForDocument(idxDoc))
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveCountStart() {
        Utils.dumpStructureToJson(idxDocumentToStartValue, pathCountStars);
    }

    public static void loadFromJsonCountStars() throws IOException {
        idxDocumentToStartValue =
                Utils.readJsonFile(pathCountStars, TYPE_MAP_WITH_COUNT_STARS);
    }

    public static void main(String[] args) {
        GenerateStartForDocument.loadGradeFromJson();
        GenerateStartForDocument.calculateStarsForAllDocument();
        GenerateStartForDocument.saveCountStart();
//        try {
//            GenerateStartForDocument.loadFromJsonCountStars();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
