package crawl;

import com.panforge.robotstxt.RobotsTxt;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

class CheckerPoliteness {
    private final HashMap<String, RobotsTxt> hostToRobots = new HashMap<>();
    private final HashMap<String, Integer> hostsDelay = new HashMap<>();
    private final LinkedHashMap<String, LocalDateTime> hostsLastQuery = new LinkedHashMap<>();
    private static final int DEFAULT_DELAY = 500;
    private static final int CONNECTION_TIMEOUT = 2000;
    private static final int CONNECTION_READ_TIMEOUT = 3000;
    private static final Logger LOGGER = Logger.getLogger(CheckerPoliteness.class);

    public void addHost(String host) {
        if (hostsLastQuery.containsKey(host)) {
            return;
        }

        hostsLastQuery.put(host, LocalDateTime.now());
        hostsDelay.put(host, getDelay(host));
    }

    String getFirstFreeHost() {
        for (Map.Entry<String, LocalDateTime> next: hostsLastQuery.entrySet()) {
            String host = next.getKey();
            if (LocalDateTime.now().compareTo(getLastUpdatePlusDelay(host)) > 0) {
                hostsLastQuery.put(host, LocalDateTime.now());
                return host;
            }
        }

        return null;
    }

    private LocalDateTime getLastUpdatePlusDelay(String host) {
        return LocalDateTime.now().plusNanos(hostsDelay.get(host) * 1000);
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


    private static RobotsMeta getRobotsMeta(Element head)
            throws IOException {
        final Elements elements = head
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

    static boolean isGoodPage(Page page) throws NotValidUploadedException {
        try {
            RobotsMeta meta = getRobotsMeta(page.getHead());
            return meta.canArchive && meta.canIndex && meta.canFollow;
        } catch (IOException e) {
            LOGGER.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    private static RobotsTxt getRobotsTxt(String protocol, String host) {
        URLConnection connection;
        try {
            connection = new URL(protocol, host, "/robots.txt").openConnection();
        } catch (IOException e) {
            LOGGER.info(e);
            if (e.getCause() != null) {
                LOGGER.info(e.getCause());
            }
            return null;
        }

        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_READ_TIMEOUT);
        try (InputStream robotsStream = connection.getInputStream()) {
            return RobotsTxt.read(robotsStream);
        } catch (RuntimeException | IOException e) {
            LOGGER.info(e);
            if (e.getCause() != null) {
                LOGGER.info(e.getCause());
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
