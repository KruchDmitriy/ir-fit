package crawl;

import db.DbConnection;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Crawler {
    static final String BOT_NAME = "irfit-bot";
    private static final Logger LOGGER = Logger.getLogger(Crawler.class);
    private static final String PATH_TO_RESOURCES = "src/main/resources/";
    private static final Path PATH_TO_DATA_SOURCE =
            Paths.get(PATH_TO_RESOURCES + "start_pages.txt");
    private static final int DEFAULT_NUM_WORKERS = 16;
    private static final String PATH_TO_EXISTS_URL = PATH_TO_RESOURCES + "existsURL.txt";
    private static final String PATH_TO_QUEUE = PATH_TO_RESOURCES + "queue.txt";
    private static final int CONNECTION_TIMEOUT = 30000;


    private final Thread[] workers;
    private final UrlContainer urlContainer;
    private final DbConnection dbConnection = new DbConnection();

    public Crawler() {
        this(DEFAULT_NUM_WORKERS, false);
    }


    public Crawler(int numWorkers, boolean startFromDump) {
        workers = new Thread[numWorkers];

        if (!startFromDump) {
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
        }else  {
            urlContainer = new UrlTimedQueue();
            startFromDump(PATH_TO_QUEUE, PATH_TO_EXISTS_URL);
        }

    }

    static Connection getConnection(String url) {
        return Jsoup.connect(url).userAgent(BOT_NAME)
                .timeout(CONNECTION_TIMEOUT);
    }

    public void start() {
        dumpingThread();
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new Worker());
            workers[i].start();
        }
    }

    public void stop() {
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    private void dump(@NotNull String fileQueue, @NotNull String fileExistsUrls) {
        urlContainer.dump(fileQueue, fileExistsUrls);
    }

    private void startFromDump(@NotNull String fileQueue, @NotNull String fileExistsUrls) {
        urlContainer.startFromDump(fileQueue, fileExistsUrls);
    }

    private void dumpingThread() {
        Thread dumpTread = new Thread(() -> {
            try {
                Thread.sleep(5 *   // minutes to sleep
                        60 *   // seconds to a minute
                        1000); // milliseconds to a second
                dump(PATH_TO_QUEUE, PATH_TO_EXISTS_URL);
            } catch (InterruptedException ex) {
                LOGGER.warn(ex.getMessage());
                dump(PATH_TO_QUEUE, PATH_TO_EXISTS_URL);
            }
        });
        dumpTread.start();
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Page currentPage = urlContainer.getUrl();
                LOGGER.info("Parsing page " + currentPage);
                for (Page page : currentPage.expandPage()) {
                    urlContainer.addUrl(page);
                }

                dbConnection.insertToUrlRow(currentPage);
            }
        }
    }
}
