package data_preprocess;

import java.util.*;
import java.util.stream.Collectors;

public class LookUpTableFreq {
    /**
     * File to freq word in this file
     */
    private Map<Integer, Long> fileToFreq = new HashMap<>();

    void addFreq(Integer fileId, Long freq) {
        fileToFreq.put(fileId, freq);
    }

    @Override
    public String toString() {
        return fileToFreq.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
