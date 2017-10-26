package crawl;

import com.panforge.robotstxt.RobotsTxt;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CheckerPoliteness {
    private final HashMap<String, RobotsTxt> hostToRobots = new HashMap<>();
    private final HashMap<String, Integer> hostsDelay = new HashMap<>();
    private final HashMap<String, LocalDateTime> hostsLastUpdate = new HashMap<>();
    private static final int DEFAULT_DELAY = 500;
    private static final int CONNECTION_TIMEOUT = 2000;
    private static final int CONNECTION_READ_TIMEOUT = 30000;
    private static final Logger LOGGER = Logger.getLogger(CheckerPoliteness.class);


    public void addStartUrls(List<URL> startUrls) {
        startUrls.forEach(url -> {
            String host = url.getProtocol() + "://" + url.getHost();
            hostsDelay.put(host, getDelay(host));
            hostToRobots.put(host, getRobotsTxt(url.getProtocol(), url.getHost()));
            hostsLastUpdate.put(host, LocalDateTime.now());
        });
    }

    int getDelay(Page page) {
        final String host = page.getHost();
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
            response = Crawler.getConnection(host + "/robots.txt").execute();
        } catch (IOException e) {
            // TODO: LOG here
            return DEFAULT_DELAY;
        }

        for (String line : response.body().split("\\n")) {
            if (line.startsWith("Crawl-delay: ")) {
                String delay = line.split(": ")[1];
                return (int) (Float.parseFloat(delay) * 1000 + 0.6);
            }
        }

        return DEFAULT_DELAY;
    }

    public LocalDateTime getLastUpdate(String host) {
        return hostsLastUpdate.get(host);
    }

    public void setLastUpdate(String host) {
        if (hostsLastUpdate.containsKey(host)) {
            hostsLastUpdate.remove(host);
        }
        hostsLastUpdate.put(host, LocalDateTime.now());
    }

    public LocalDateTime getLastUpdatePlusDelay(Page page) {
        return getLastUpdate(page.getHost()).plusNanos(getDelay(page) * 1000);
    }

    public boolean canUpdate(Page page) {
        return getLastUpdatePlusDelay(page).compareTo(LocalDateTime.now()) < 0;
    }

    boolean hasAccess(Page page) {
        final URL pageUrl = page.getUrl();
        final RobotsTxt robotsTxt;
        if (hostToRobots.containsKey(page.getHost())) {
            robotsTxt = hostToRobots.get(page.getHost());
        } else {
            robotsTxt = getRobotsTxt(pageUrl.getProtocol(), pageUrl.getHost());
            hostToRobots.put(page.getHost(), robotsTxt);
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

    private static RobotsTxt getRobotsTxt(String protocol, String host) {
        URLConnection connection;
        try {
            connection = new URL(protocol, host, "/robots.txt").openConnection();
        } catch (IOException e) {
            LOGGER.warn(e);
            if (e.getCause() != null) {
                LOGGER.warn(e.getCause());
            }
            return null;
        }

        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_READ_TIMEOUT);
        try (InputStream robotsStream = connection.getInputStream()) {
            return RobotsTxt.read(robotsStream);
        } catch (RuntimeException | IOException e) {
            LOGGER.warn(e);
            if (e.getCause() != null) {
                LOGGER.warn(e.getCause());
            }
            return null;
        }
    }

    static class RobotsMeta {
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
