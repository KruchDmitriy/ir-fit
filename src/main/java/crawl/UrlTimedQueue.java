package crawl;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class UrlTimedQueue implements UrlContainer {
    private final PriorityQueue<Page> uris = new PriorityQueue<>(Page::compareViaDateToUpload);
    private final HashSet<String> seenUris = new LinkedHashSet<>();
    private final CheckerPoliteness checkerPoliteness;
    private static final Logger LOGGER = Logger.getLogger(UrlTimedQueue.class);
    private final URI basePage;

    UrlTimedQueue() {
        try {
            basePage = new URI("http://www.google.com/");
        } catch (URISyntaxException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
        checkerPoliteness = new CheckerPoliteness();
    }

    UrlTimedQueue(List<URL> startUrls) {
        this();

        startUrls.forEach(url -> {
            try {
                seenUris.add(url.toURI().toString());
                putPage(new Page(url, basePage.toURL()));
            } catch (URISyntaxException | MalformedURLException e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        });

        checkerPoliteness.addStartUrls(startUrls);
    }

    @Override
    public synchronized Page getUrl() {
        while (true) {
            final Page page = uris.poll();
            if (!checkerPoliteness.canUpdate(page)) {
                page.setDateToUpload(
                        checkerPoliteness.getLastUpdatePlusDelay(page));
                uris.add(page);
                continue;
            }

            checkerPoliteness.setLastUpdate(page.getHost());
            return page;
        }
    }

    @Override
    public synchronized void addUrl(Page page) {
        if (seenUris.contains(page.getUri().toString())) {
            return;
        }

        if (!checkerPoliteness.hasAccess(page)) {
            return;
        }

        putPage(page);
    }

    private void putPage(Page page) {
        seenUris.add(page.getUri().toString());
        checkerPoliteness.setLastUpdate(page.getHost());
        LocalDateTime time = checkerPoliteness.getLastUpdatePlusDelay(page);
        page.setDateToUpload(time);
        uris.add(page);
    }

    @Override
    public void dump(@NotNull String fileQueue, @NotNull String fileExistsUrl) {
        try (Writer writerQueue =
                     new BufferedWriter(new OutputStreamWriter(
                             new FileOutputStream(fileQueue)));
             Writer existsUrl = new BufferedWriter(new OutputStreamWriter(
                     new FileOutputStream(fileExistsUrl)))) {
            uris.forEach(page -> {
                try {
                    writerQueue.write(page.toString() + "\n");
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            });

            seenUris.forEach(uri -> {
                try {
                    existsUrl.write(uri + "\n");
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            });

        } catch (IOException ex) {
            LOGGER.warn(ex.getMessage());
        }
    }

    @Override
    public void startFromDump(@NotNull String fileQueue, @NotNull String fileExistsUrl) {
        try (Stream<String> streamQueue = Files.lines(Paths.get(fileQueue));
             Stream<String> streamExistsUrl = Files.lines(Paths.get(fileQueue))) {

            streamExistsUrl.forEach(seenUris::add);

            streamQueue.forEach(line -> {
                try {
                    Page currentPage = new Page(new URL(line), basePage.toURL());
                    putPage(currentPage);
                } catch (URISyntaxException | MalformedURLException ex) {
                    LOGGER.error(ex.getMessage());
                }
            });
        } catch (IOException ignored) {
        }
    }
}
