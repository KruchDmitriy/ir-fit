package crawl;

import db.DbConnection;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class Crawler {
    static final String BOT_NAME = "irfit-bot";
    private static final Logger LOGGER = Logger.getLogger(Crawler.class);
    static final String PATH_TO_RESOURCES = "src/main/resources/";
    private static final int DEFAULT_NUM_WORKERS = 16;
    private static final String PATH_TO_DATA = "../ir-fit-data/";
    private static final String PATH_TO_PAGES = PATH_TO_DATA + "documents/";
    private static final String PATH_TO_TEXTS = PATH_TO_DATA + "texts/";
    private static final int CONNECTION_TIMEOUT = 3000;

    private final HashSet<String> pageHashCodes = new HashSet<>();
    private static final String PATH_TO_HASH = PATH_TO_RESOURCES + "hash.dump";

    private final Thread[] workers;
    private final UrlContainer urlContainer;
    private final DbConnection dbConnection = new DbConnection();

    public Crawler() {
        this(DEFAULT_NUM_WORKERS);
    }

    public Crawler(int numWorkers) {
        workers = new Thread[numWorkers];
        urlContainer = new UrlTimedQueue();
        readDump();
        startDumper();
    }

    static Connection getConnection(String url) {
        return Jsoup.connect(url).userAgent(BOT_NAME)
                .timeout(CONNECTION_TIMEOUT);
    }

    public void start() {
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

    private void startDumper() {
        new Dumper(this::writeDump);
    }

    private void writeDump() {
        try (DataOutputStream hashDataOutputStream = new DataOutputStream(new FileOutputStream(PATH_TO_HASH))) {
            hashDataOutputStream.writeInt(pageHashCodes.size());
            pageHashCodes.forEach(hash -> {
                try {
                    writeString(hashDataOutputStream, hash);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    static void writeString(DataOutputStream dataOutputStream, String string) throws IOException {
        dataOutputStream.writeInt(string.length());
        dataOutputStream.writeChars(string);
    }

    static String readString(DataInputStream dataInputStream) throws IOException {
        int length = dataInputStream.readInt();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = dataInputStream.readChar();
        }

        return String.valueOf(chars);
    }

    private void readDump() {
        if (!Files.exists(Paths.get(PATH_TO_HASH))) {
            return;
        }

        try (DataInputStream hashDataInputStream = new DataInputStream(new FileInputStream(PATH_TO_HASH))) {
            final int hashSize = hashDataInputStream.readInt();
            for (int i = 0; i < hashSize; i++) {
                String hash = readString(hashDataInputStream);
                pageHashCodes.add(hash);
            }
        } catch (IOException e) {
            LOGGER.warn(e.toString());
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Page currentPage = urlContainer.getUrl();
                LOGGER.warn("Parsing page " + currentPage);
                try {
                    if (!CheckerPoliteness.isGoodPage(currentPage)
                        || !SearchManager.isGoodPage(currentPage)) {
                        LOGGER.warn("Bad page! Url: " + currentPage.toString());
                        continue;
                    }

                    final String hash = currentPage.hash();
                    if (pageHashCodes.contains(hash)) {
                        LOGGER.warn("Hash already was! Url: " + currentPage.toString());
                        continue;
                    }

                    pageHashCodes.add(hash);

                    for (Page page : currentPage.expandPage()) {
                        urlContainer.addUrl(page);
                    }

                    final String fileName = writeToFile(currentPage);
                    dbConnection.insertToUrlRow(currentPage, fileName);
                } catch (NotValidUploadedException e) {
                    LOGGER.info("Page " + currentPage + "wasn't uploaded properly");
                }
            }
        }
    }

    private String writeToFile(Page page) throws NotValidUploadedException {
        final String fileName = page.getUrl().toString().replaceAll("/", "_");
        final String pathToDocument = PATH_TO_PAGES + fileName;
        final String pathToText = PATH_TO_TEXTS + fileName;

        try {
            Files.write(Paths.get(pathToDocument), page.getBody().getBytes(), StandardOpenOption.CREATE_NEW);
            Files.write(Paths.get(pathToText), page.getText().getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            LOGGER.warn(e.toString(), e);
        }

        return fileName;
    }
}
