package search;

import com.google.gson.reflect.TypeToken;
import db.DbConnection;
import features.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class LoadUrls {

    private static final String idxToUrlDoc =
            "./src/main/resources/idxToUrl.json";

    private static ConcurrentHashMap<Integer, String> idxDocumentToOriginUrl;

    public static void loadUrls() {
        DbConnection connection = new DbConnection();
        idxDocumentToOriginUrl = connection.readAllTableUrls();
    }

    public static ConcurrentHashMap<Integer, String> getIdxDocumentToOriginUrl() {
        return idxDocumentToOriginUrl;
    }


    public static void loadJsonFileWithIdxToUrlOriginAddress() {
        try {

            Type type = new TypeToken<ConcurrentHashMap<Integer, String>>() {
            }.getType();
            idxDocumentToOriginUrl =
                    Utils.readJsonFile(idxToUrlDoc,
                            type);
        } catch (IOException e) {
            System.out.println("load file " + idxToUrlDoc + " failed");
        }
    }

    public static void main(String[] args) {
//        LoadUrls.loadUrls();
//        Utils.dumpStructureToJson(LoadUrls.getIdxDocumentToOriginUrl(), idxToUrlDoc);
        LoadUrls.loadJsonFileWithIdxToUrlOriginAddress();
        int a = 0;
    }
}
