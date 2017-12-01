package search;

import features.FindGrade;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateStartForDocument {

    private static final String pathGradeJson =
            "./src/main/resources/gradeFind.json";

    private static final ConcurrentHashMap<Integer, Double>
            idxDocumentToStartValue = new ConcurrentHashMap<>();


    private static final FindGrade grade = new FindGrade();

    public static void loadGradeFromJson() {
        grade.loadGradeFromJson();
    }

}
