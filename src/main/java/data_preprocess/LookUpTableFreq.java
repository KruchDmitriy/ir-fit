package data_preprocess;

import java.util.HashMap;
import java.util.Map;

public class LookUpTableFreq {
    // file to freq word in this file
    private Map<String, Long> fileToFreq = new HashMap<>();

    void addFreq(String file, Long freq) {
        fileToFreq.put(file, freq);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        fileToFreq.forEach((fileName, freqWord) ->
                builder
                .append(fileName).append(":")
                .append(freqWord).append(";"));
        return builder.toString();
    }
}
