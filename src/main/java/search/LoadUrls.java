package search;

import db.DbConnection;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class LoadUrls {

    private static ConcurrentHashMap<Integer, String> idxDocumentToOriginUrl;

    public static void loadUrls() throws IOException {
        DbConnection connection = new DbConnection();
        idxDocumentToOriginUrl = connection.readAllTableUrls();
    }

    public static ConcurrentHashMap<Integer, String> getIdxDocumentToOriginUrl() {
        return idxDocumentToOriginUrl;
    }

    public static void main(String[] args) throws IOException {
        LoadUrls.loadUrls();
    }
}
