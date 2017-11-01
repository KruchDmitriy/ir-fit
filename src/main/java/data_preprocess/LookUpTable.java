package data_preprocess;

import java.util.*;

public class LookUpTable {
    // file name to list index word in this file
    private Map<String, List<Integer>> fileToListPositionWords = new HashMap<>();

    void addListPositionInFile(String fileName, List<Integer> integerList) {
        if (!fileToListPositionWords.containsKey(fileName)) {
            fileToListPositionWords.put(fileName, new ArrayList<>());
        }
        fileToListPositionWords.get(fileName).addAll(integerList);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        fileToListPositionWords.forEach((file, intList) -> {
                    if (!intList.isEmpty()) {
                        builder.append(file).append(":")
                                .append(intList.get(0));
                        intList.subList(1, intList.size()).forEach(integer ->
                                builder.append(",").append(integer));
                        builder.append(";");
                    }
                }
        );
        return builder.toString();
    }
}
