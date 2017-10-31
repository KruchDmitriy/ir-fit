package data_preprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by kate on 31.10.17.
 */
public class LookUpTable {
    private Map<String, List<Integer>> fileToListPositionWords = new HashMap<>();

//    public void addFile(Stream<String> files) {
//        files.forEach(file -> fileToListPositionWords.put(file, Collections.emptyList()));
//    }
//
//    public void addPositionInFile(String fileName, Integer position) {
//        fileToListPositionWords.get(fileName).add(position);
//    }

    public void addListPositionInFile(String fileName, List<Integer> integerList) {
        fileToListPositionWords.get(fileName).addAll(integerList);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        fileToListPositionWords.forEach((file, intList) -> {
                    builder.append(file).append(":");
                    intList.forEach(integer ->
                            builder.append(integer).append(",")
                    );
                    builder.append(";");
                }
        );
        return builder.toString();
    }
}
