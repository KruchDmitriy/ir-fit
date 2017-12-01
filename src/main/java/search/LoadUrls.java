package search;

import db.DbConnection;

import java.util.concurrent.ConcurrentHashMap;

public class LoadUrls {

    private static ConcurrentHashMap<Integer, String> idxDocumentToOriginUrl;

    public static void loadUrls() {
        DbConnection connection = new DbConnection();
        idxDocumentToOriginUrl = connection.readAllTableUrls();
    }

    public static ConcurrentHashMap<Integer, String> getIdxDocumentToOriginUrl() {
        return idxDocumentToOriginUrl;
    }

    public static void main(String[] args) {
        LoadUrls.loadUrls();
    }
}
