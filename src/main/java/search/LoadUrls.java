package search;


import db.DbConnection;

public class LoadUrls {

    public static void loadUrls() {
        DbConnection connection = new DbConnection();
        connection.readAllTableUrls();
    }

    public static void main(String[] args) {
        LoadUrls.loadUrls();
    }
}
