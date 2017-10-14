package crawl;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class UrlTimedQueue implements UrlContainer {
    private final TreeMap<ExecutionTime, Page> urls = new TreeMap<>();
    private final HashSet<URI> seenUrls = new LinkedHashSet<>();
    private final CheckerPoliteness checkerPoliteness;
    private static final Logger LOGGER = Logger.getLogger(UrlTimedQueue.class);
    private final URI basePage;

    UrlTimedQueue(List<URL> startUrls) {
        try {
            basePage = new URI("http://www.google.com");
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }

        startUrls.forEach(url -> {
            try {
                seenUrls.add(url.toURI());
                putPage(LocalDateTime.now(), new Page(url.toURI(), basePage));
            } catch (URISyntaxException e) {
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        });
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
        LocalDateTime downloadDate = getDateAfterMillis(page.getUploadDate(), delay);
        putPage(downloadDate, page);
    }

    private static LocalDateTime getDateAfterMillis(LocalDateTime current, int milliseconds) {
        return current.plusNanos(milliseconds * 1000);
    }

    private void putPage(LocalDateTime time, Page page) {
        urls.put(new ExecutionTime(time, page), page);
    }

    private static class ExecutionTime implements Comparable<ExecutionTime> {
        final LocalDateTime time;
        final int order;

        ExecutionTime(LocalDateTime time, Page page) {
            this.time = time;
            order = page.toString().hashCode();
        }

        @Override
        public int compareTo(@NotNull ExecutionTime o) {
            int compare = time.compareTo(o.time);
            if (compare != 0) {
                return compare;
            }

            return order - o.order;
        }
    }
}
