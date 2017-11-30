package data_preprocess;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LookUpTableFreq {
    /**
     * File to freq word in this file
     */
    private Map<String, Long> fileToFreq = new HashMap<>();

    void addFreq(String file, Long freq) {
        fileToFreq.put(file, freq);
    }

    @Override
    public String toString() {
        return fileToFreq.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
