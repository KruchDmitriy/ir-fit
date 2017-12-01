package data_preprocess;

import java.util.*;
import java.util.stream.Collectors;

public class LookUpTableFreq {
    /**
     * File to freq word in this file
     */
    private Map<Integer, Integer> fileToFreq = new LinkedHashMap<>();

    void addFreq(Integer fileId, Integer freq) {
        fileToFreq.put(fileId, freq);
    }

    int getFreq(Integer fileId) {
        return fileToFreq.getOrDefault(fileId, 0);
    }

    Set<Integer> getAllFiles() {
        return fileToFreq.keySet();
    }

    int size() {
        return fileToFreq.size();
    }

    @Override
    public String toString() {
        return fileToFreq.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
