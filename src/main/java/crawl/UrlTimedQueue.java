package crawl;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.util.*;

public class UrlTimedQueue implements UrlContainer {

    private static final Logger LOGGER = Logger.getLogger(UrlTimedQueue.class);

    private final TreeMap<Date, Page> urls = new TreeMap<>();
    private final HashSet<URI> seenUrls = new LinkedHashSet<>();
    private final CheckerPoliteness checkerPoliteness;

    UrlTimedQueue(List<URI> startUrls) {
        seenUrls.addAll(startUrls);
        checkerPoliteness = new CheckerPoliteness(startUrls);
    }

    @Override
    public synchronized Page getUrl() {
        Page page = urls.firstEntry().getValue();
        urls.remove(urls.firstKey());
        return page;
    }

    @Override
    public synchronized void addUrl(Page page) {
        if (seenUrls.contains(page.getUrl())) {
            return;
        }

        if (!checkerPoliteness.hasAccess(page)) {
            return;
        }

        int delay = checkerPoliteness.getDelay(page);
        Date downloadDate = getDateAfterMillis(page.getUploadDate(), delay);
        urls.put(downloadDate, page);
    }

    private static Date getDateAfterMillis(Date current, int milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.MILLISECOND, milliseconds);
        return calendar.getTime();
    }

    @Override
    public void dump(@NotNull String fileQueue, @NotNull String fileExistsUrl) {
        try (Writer writerQueue =
                     new BufferedWriter(new OutputStreamWriter(
                             new FileOutputStream(fileQueue))) ;
             Writer existsUrl =        new BufferedWriter(new OutputStreamWriter(
                     new FileOutputStream(fileExistsUrl)))  ) {
            urls.forEach((date, page) -> {
                try {
                    writerQueue.write(page.toString() + "\n");
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            });

            seenUrls.forEach(uri -> {
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
}
