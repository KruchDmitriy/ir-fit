package crawl;

import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CheckerPoliteness {
    private final HashMap<String, RobotsTxt> hostToRobots = new HashMap<>();
    private final HashMap<String, Integer> hostsDelay = new HashMap<>();
    private static final int DEFAULT_DELAY = 500;

    CheckerPoliteness(List<URI> startUrls) {
        startUrls.forEach(url -> {
            String host = url.getHost();
            hostsDelay.put(host, getDelay(host));
            hostToRobots.put(host, getRobotsTxt(host));
        });
    }

    int getDelay(Page page) {
        final String host = page.getUrl().getHost();
        if (hostsDelay.containsKey(host)) {
            return hostsDelay.get(host);
        }

        int delay = getDelay(host);
        hostsDelay.put(host, delay);
        return delay;
    }

    /**
     * @param host -- url without context path
     * @return delay to this host in milliseconds
     */
    private static int getDelay(String host) {
        Connection.Response response;
        try {
            response = Crawler.getConnection( host + "/robots.txt").execute();
        } catch (IOException e) {
            // TODO: LOG here
            return DEFAULT_DELAY;
        }

        for (String line: response.body().split("\\n")) {
            if (line.startsWith("Crawl-delay: ")) {
                String delay = line.split(": ")[1];
                return (int) (Float.parseFloat(delay) * 1000 + 0.6);
            }
        }

        return DEFAULT_DELAY;
    }

    boolean hasAccess(Page page) {
        String host = page.getUrl().getHost();
        final RobotsTxt robotsTxt;
        if (hostToRobots.containsKey(host)) {
            robotsTxt = hostToRobots.get(host);
        } else {
            robotsTxt = getRobotsTxt(page.getUrl().getHost());
            hostToRobots.put(host, robotsTxt);
        }

        return robotsTxt == null ||
                robotsTxt.query(Crawler.BOT_NAME, page.toString());
    }


    static RobotsMeta getRobotsMeta(Document document)
            throws IOException {
        final Elements elements = document.head()
                .getElementsByAttributeValue("name", "robots");

        Set<String> contents = elements.stream()
                .map(element -> element.attr("content")
                        .toUpperCase()
                        .split(",\\? "))
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        boolean canIndex = !contents.contains("NOINDEX");
        boolean canFollow = !contents.contains("NOFOLLOW");
        boolean canArchive = !contents.contains("NOARCHIVE");

        return new RobotsMeta(canIndex, canFollow, canArchive);
    }

    private static RobotsTxt getRobotsTxt(String host) {
        try (InputStream robotsStream =
                     new URL(host + "/robots.txt").openStream()) {
            return RobotsTxt.read(robotsStream);
        } catch (IOException e) {
            // TODO: LOG here
            return null;
        }
    }

    private static class RobotsMeta {
        final boolean canIndex;
        final boolean canFollow;
        final boolean canArchive;

        RobotsMeta(boolean canIndex, boolean canFollow, boolean canArchive) {
            this.canIndex = canIndex;
            this.canFollow = canFollow;
            this.canArchive = canArchive;
        }
    }
}
