package crawl;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static crawl.Crawler.PATH_TO_RESOURCES;
import static crawl.Crawler.readString;
import static crawl.Crawler.writeString;
import static java.nio.file.StandardOpenOption.WRITE;

public class UrlTimedQueue implements UrlContainer {
    private static final int MAX_QUEUE_SIZE = 30_000;
    private static final Logger LOGGER = Logger.getLogger(UrlTimedQueue.class);
    private final HashMap<String, Queue<Page>> hostToPages = new HashMap<>();
    private final HashSet<String> seenPages = new HashSet<>();
    private final QueueCacher cacher = new QueueCacher();
    private final CheckerPoliteness checkerPoliteness;

    private static final String PATH_TO_QUEUE = PATH_TO_RESOURCES + "queue.dump";

    UrlTimedQueue() {
        checkerPoliteness = new CheckerPoliteness();
        startCheckingConsistencyThreads();
        startFromDump(PATH_TO_QUEUE);
    }

    @Override
    public synchronized Page getUrl() {
        if (hostToPages.size() == 0) {
            readPages();
        }

        while (true) {
            String freeHost = checkerPoliteness.getFirstFreeHost();
            if (freeHost == null) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) { return null; }
                continue;
            }

            Queue<Page> pages = hostToPages.get(freeHost);
            if (pages.isEmpty()) {
                hostToPages.remove(freeHost);
                checkerPoliteness.removeHost(freeHost);
                continue;
            }

            return pages.poll();
        }
    }

    @Override
    public synchronized void addUrl(Page page) {
        if (!checkerPoliteness.hasAccess(page) ||
            seenPages.contains(page.toString())) {
            return;
        }
        seenPages.add(page.toString());

        if (hostToPages.size() > MAX_QUEUE_SIZE) {
            cacher.dumpPage(page);
            return;
        }

        final String host = page.getHost();
        checkerPoliteness.addHost(host);
        if (!hostToPages.containsKey(host)) {
            Queue<Page> queue = new ArrayDeque<>();
            queue.add(page);
            hostToPages.put(host, queue);
        } else {
            final Queue<Page> pages = hostToPages.get(host);
            pages.add(page);
        }
    }

    private synchronized void readPages() {
        cacher.getPages()
                .forEach(page -> hostToPages.get(page.getHost()).add(page));
    }

    private void startCheckingConsistencyThreads() {
        startDumpingThread();
        startCacherThread();
    }

    private void startCacherThread() {
        Thread threadCacher = new Thread(cacher);
        threadCacher.setDaemon(true);
        threadCacher.start();
    }

    private void startDumpingThread() {
        new Dumper(this::dump).start();
    }

    private void dump() {
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(PATH_TO_QUEUE))) {

            dataOutputStream.writeInt(hostToPages.size());
            hostToPages.forEach((host, listPages) -> {
                try {
                    writeString(dataOutputStream, host);
                    dataOutputStream.writeInt(listPages.size());
                    listPages.forEach(
                        page -> {
                            try {
                                writeString(dataOutputStream, page.getUrl().toString());
                                writeString(dataOutputStream, page.getParentUrl().toString());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            dataOutputStream.writeInt(seenPages.size());
            seenPages.forEach(page -> {
                try {
                    writeString(dataOutputStream, page);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    private void startFromDump(@NotNull String queueFile) {
        if (!Files.exists(Paths.get(queueFile))) {
            return;
        }

        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(queueFile))) {
            final int hashMapSize = dataInputStream.readInt();

            for (int i = 0; i < hashMapSize; i++) {
                String host = readString(dataInputStream);
                if (!hostToPages.containsKey(host)) {
                    hostToPages.put(host, new ArrayDeque<>());
                }

                int length = dataInputStream.readInt();
                for (int j = 0; j < length; j++) {
                    String url = readString(dataInputStream);
                    String baseUrl = readString(dataInputStream);

                    addUrl(new Page(new URL(url), new URL(baseUrl)));
                }
            }

            final int seenPagesSize = dataInputStream.readInt();
            for (int i = 0; i < seenPagesSize; i++) {
                seenPages.add(readString(dataInputStream));
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.warn(e.toString());
        }
    }

    private class QueueCacher implements Runnable {
        private final BlockingQueue<Page> queue = new LinkedBlockingQueue<>();
        private AtomicInteger size = new AtomicInteger();
        private static final int CACHE_QUEUE_SIZE = 50;
        private Path QUEUE_CACHE_FILE = Paths.get(PATH_TO_RESOURCES + "queue_cache.txt");
        private Path TMP_FILE = Paths.get(PATH_TO_RESOURCES + "queue_tmp.txt");

        {
            if (!Files.exists(QUEUE_CACHE_FILE)) {
                try {
                    Files.createFile(QUEUE_CACHE_FILE);
                } catch (IOException e) {
                    LOGGER.error(e.toString(), e);
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while (size.get() < CACHE_QUEUE_SIZE) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            LOGGER.info("Queue cacher has interrupted.");
                            return;
                        }
                    }

                    for (int i = 0; i < size.get(); i++) {
                        final Page page = queue.poll();
                        size.decrementAndGet();
                        final String toFile = page.getUrl().toString() + " " + page.getParentUrl().toString();
                        try {
                            Files.write(QUEUE_CACHE_FILE, toFile.getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            LOGGER.error("Dumper fail to write to file, exception: " + e.toString(), e);
                        }
                    }
                }
            }
        }

        Stream<Page> getPages() {
            try {
                Files.write(TMP_FILE, "".getBytes(), WRITE);

                final long length = Files.lines(QUEUE_CACHE_FILE).count();

                return Files.lines(QUEUE_CACHE_FILE)
                        .map(new Function<String, Page>() {
                            private int counter = 0;

                            @Override
                            public Page apply(String line) {
                                counter++;
                                if (counter < MAX_QUEUE_SIZE) {
                                    String[] split = line.split(" ");
                                    try {
                                        return new Page(new URL(split[0]), new URL(split[1]));
                                    } catch (URISyntaxException | MalformedURLException e) {
                                        LOGGER.info(e.toString());
                                    }
                                } else {
                                    try {
                                        Files.write(TMP_FILE, line.getBytes(), StandardOpenOption.APPEND);
                                    } catch (IOException e) {
                                        LOGGER.error(e.toString(), e);
                                    }

                                    if (counter == length) {
                                        try {
                                            Files.copy(TMP_FILE, QUEUE_CACHE_FILE, StandardCopyOption.REPLACE_EXISTING);
                                        } catch (IOException e) {
                                            LOGGER.error(e.toString(), e);
                                        }
                                    }
                                }
                                return null;
                            }
                        }).filter(Objects::nonNull);
            } catch (IOException e) {
                LOGGER.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        void dumpPage(Page page) {
            queue.add(page);

            if (size.incrementAndGet() >= CACHE_QUEUE_SIZE) {
                synchronized (queue) {
                    queue.notify();
                }
            }
        }
    }

    private void generateFirstDump() throws IOException {
        final String startPagesFile = PATH_TO_RESOURCES + "start_pages.txt";
        final URL google = new URL("http://www.google.com/");

        Files.lines(Paths.get(startPagesFile))
                .forEach(line -> {
                    try {
                        addUrl(new Page(new URL(line), google));
                    } catch (URISyntaxException | MalformedURLException e) {
                        LOGGER.error(e.toString(), e);
                    }
                });
        dump();
    }

    public static void main(String[] args) throws IOException {
        UrlTimedQueue queue = new UrlTimedQueue();
        queue.generateFirstDump();
    }
}
