package crawl;

import java.net.URI;
import java.util.*;

public class UrlTimedQueue implements UrlContainer {
    private final TreeMap<Date, Page> urls = new TreeMap<>();
    private final HashSet<URI> seenUrls = new LinkedHashSet<>();
    private final CheckerPoliteness checkerPoliteness;

    UrlTimedQueue(List<URI> startUrls) {
        seenUrls.addAll(startUrls);
        checkerPoliteness = new CheckerPoliteness(startUrls);
    }

    @Override
    public Page getUrl() {
        Page page = urls.firstEntry().getValue();
        urls.remove(urls.firstKey());
        return page;
    }

    @Override
    public void addUrl(Page page) {
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
}
