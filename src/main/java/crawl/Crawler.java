package crawl;

import db.DbConnection;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Crawler {
    static final String BOT_NAME = "irfit-bot";
    private static final Path PATH_TO_DATA_SOURCE =
            Paths.get("src/main/resources/start_pages.txt");
    private static final int DEFAULT_NUM_WORKERS = 16;
    private static final Logger LOGGER = Logger.getLogger(Crawler.class);
    private final Thread[] workers;
    private final UrlContainer urlContainer;
    private final DbConnection dbConnection = new DbConnection();

    public Crawler() {
        this(DEFAULT_NUM_WORKERS);
    }

    public Crawler(int numWorkers) {
        workers = new Thread[numWorkers];
        List<URL> startUrls;
        try {
             startUrls = Files.lines(PATH_TO_DATA_SOURCE)
                    .map(line -> {
                        try {
                            return new URL(line);
                        } catch (MalformedURLException e) {
                            LOGGER.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error while reading file start_pages.txt", e);
            throw new RuntimeException(e);
        }

        urlContainer = new UrlTimedQueue(startUrls);
    }

    static Connection getConnection(String url) {
        return Jsoup.connect(url).userAgent(BOT_NAME);
    }

    public void start() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new Worker());
            workers[i].start();
        }
    }

    public void stop() {
        for (Thread worker: workers) {
            worker.interrupt();
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Page currentPage = urlContainer.getUrl();

                LOGGER.info(currentPage);

                for (Page page: currentPage.expandPage()) {
                    urlContainer.addUrl(page);
                }

                dbConnection.insertToUrlRow(currentPage);
            }
        }
    }
}
