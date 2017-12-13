package features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import features.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MakeTitleMap {
    private static final String titles = "../../../titles/";

    public static void main(String[] args) {

        try {
            Map<Integer, String> idFileToTitle = new HashMap<>();
            Utils.loadArrayWithNameFiles();
            Map<String, Integer> nameToID = Utils.getNameDocumentToIndex();

            data_preprocess.utils.Utils.getAllFiles(Paths.get(titles))
                    .forEach(nameFile -> {
                        try {
                            if (!nameToID.containsKey(nameFile.getFileName()
                                    .toString())) {
                                System.out.println("not find : " + nameFile
                                        .getFileName
                                        ().toString());
                                return;
                            }
                            idFileToTitle.put(nameToID.get(nameFile
                                    .getFileName().toString()), new
                                    String(Files.readAllBytes(nameFile))
                                    .replaceAll("\\p{Punct}","")
                                    .replaceAll("\n", ""));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            System.out.println("save ... ");
            Utils.dumpStructureToJson(idFileToTitle, "../../../title.json");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
