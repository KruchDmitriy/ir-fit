package data_preprocess;

import java.util.*;

public class LookUpTable {
    /**
     * File name to list index word in this file
      */
    private Map<Integer, List<Integer>> fileToListPositionWords = new HashMap<>();

    void addListPositionInFile(Integer fileId, List<Integer> listPosition) {
        assert(!fileToListPositionWords.containsKey(fileId));
        fileToListPositionWords.put(fileId, listPosition);
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
