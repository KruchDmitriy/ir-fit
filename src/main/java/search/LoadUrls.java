package search;

import db.DbConnection;
import features.utils.Utils;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class LoadUrls {

    private static final String idxToUrlDoc =
            "./src/main/resources/idxToUrl.json";

    private static ConcurrentHashMap idxDocumentToOriginUrl;

    public static void loadUrls() {
        DbConnection connection = new DbConnection();
        idxDocumentToOriginUrl = connection.readAllTableUrls();
    }

    public static ConcurrentHashMap<Integer, String> getIdxDocumentToOriginUrl() {
        return idxDocumentToOriginUrl;
    }

    public static void loadJsonFileWithIdxToUrlOriginAddress() {
        try {
            idxDocumentToOriginUrl =
                    Utils.readJsonFile(idxToUrlDoc, ConcurrentHashMap.class);
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
