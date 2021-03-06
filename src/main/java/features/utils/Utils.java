package features.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data_preprocess.InvertIndex;
import org.jetbrains.annotations.NotNull;


import java.io.*;
import java.lang.reflect.Type;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {
    private static final String pathToNameDocument = "../../../files.json";
    private static ConcurrentHashMap<String, Integer> nameDocumentToIndex = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadArrayWithNameFiles() throws IOException {
        List<String> nameDocument = InvertIndex.readFileIndex(pathToNameDocument);

        if (nameDocument == null) {
            System.out.println(" document name not found ");
            return;
        }
        for (int idx = 0; idx < nameDocument.size(); idx++) {
            nameDocumentToIndex.put(nameDocument.get(idx), idx);
        }
    }

    public static ConcurrentHashMap<String, Integer> getNameDocumentToIndex() {
        return nameDocumentToIndex;
    }

    public static void dumpStructureToJson(Object o, String pathToOutFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter
                (pathToOutFile))) {
            GSON.toJson(o, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T readJsonFile(@NotNull String pathToJsonFile,
                                     Type returnObjClass)
            throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(pathToJsonFile))) {
            return GSON.fromJson(reader, returnObjClass);
        }
    }
}
