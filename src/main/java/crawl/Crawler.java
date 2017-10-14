package crawl;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.Map;

public class Crawler {
    static final String NAME = "irfit-bot";

    static Connection getConnection(String url) {
        return Jsoup.connect(url).userAgent(NAME);
    }

    // dirty experiments =(
    public static void main(String[] args) throws IOException {
        Connection connection = Jsoup.connect("http://google.com");
        connection.userAgent(NAME);
        Connection.Response response = connection.execute();

        for (Map.Entry<String, String> mapEntry : response.headers().entrySet()) {
            System.out.println(mapEntry.getKey() + ": " + mapEntry.getValue());
        }

//        final List<String> contents = robotsContent(response);
//        for (String content : contents) {
//            System.out.println(content);
//        }
    }

    private static class Worker implements Runnable {
        @Override
        public void run() {

        }
    }
}
