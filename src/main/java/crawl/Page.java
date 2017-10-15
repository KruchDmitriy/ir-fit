package crawl;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Page {
    private final URL url;
    private final URL parentUrl;

    private final URI uri;
    private final URI parentUri;

    private LocalDateTime creationDate;
    private LocalDateTime dateToUpload;
    private Element body;
    private static final Logger LOGGER = Logger.getLogger(Page.class);
    private boolean isValidUploaded = false;
    private static final String NOT_VALID_UPLOADED_BODY = "notValidUploaded";

    Page(URL url, URL baseUrl) throws URISyntaxException {
        this.url = url;
        this.parentUrl = baseUrl;
        this.uri = url.toURI();
        this.parentUri = baseUrl.toURI();
        creationDate = LocalDateTime.now();
    }

    public URL getUrl() {
        return url;
    }

    public String getHost() {
        return url.getProtocol() + "://" + url.getHost();
    }

    public URL getParentUrl() {
        return parentUrl;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getBody() {
        if (body == null) {
            downloadPage();
        }

        if (!isValidUploaded) {
            return NOT_VALID_UPLOADED_BODY;
        }

        return body.html();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public List<Page> expandPage() {
        downloadPage();
        if (!isValidUploaded) {
            return (List<Page>) Collections.EMPTY_LIST;
        }

        return body.getElementsByTag("a")
                .stream()
                .map(tag -> tag.attr("href"))
                .map(link -> {
                    try {
                        URI uri = normalizedUri(link, null);
                        if (!link.startsWith("http")) {
                            uri = normalizedUri(url.toString(), link);
                        }
                        return new Page(uri.toURL(), url);
                    } catch (URISyntaxException
                            | MalformedURLException
                            | UnsupportedEncodingException e) {
                        LOGGER.warn("Bad page: " + e + "\n" + link);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public boolean isValidUploaded() {
        return isValidUploaded;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Page) && ((Page) obj).getUrl().equals(url);
    }

    @Override
    public int hashCode() {
        if (body == null) {
            downloadPage();
        }

        if (!isValidUploaded) {
            return NOT_VALID_UPLOADED_BODY.hashCode();
        }

        return body.toString().hashCode();
    }

    public URI getUri() {
        return uri;
    }

    public void setDateToUpload(LocalDateTime dateToUpload) {
        this.dateToUpload = dateToUpload;
    }

    public static int compareViaDateToUpload(Page page1, Page page2) {
        int compare = page1.dateToUpload.compareTo(page2.dateToUpload);
        if (compare != 0) {
            return compare;
        }

        return (int) Math.signum(page1.getUrl().hashCode() -
                page2.getUrl().hashCode());
    }

    private void downloadPage() {
        if (body != null) {
            return;
        }

        try {
            Connection.Response response = Crawler.getConnection(url.toString()).execute();
            creationDate = LocalDateTime.now();
            body = response.parse().body();
            isValidUploaded = true;
        } catch (IOException e) {
            LOGGER.error("Error while downloading page " + url.toString() + e);
            isValidUploaded = false;
        }
    }

    private static boolean checkExtension(String uri) {
        String extension = uri.substring(uri.lastIndexOf(".") + 1);
        return extension.equals("html");
    }


    private static URI normalizedUri(String firstPart, String relative) throws URISyntaxException,
            MalformedURLException, UnsupportedEncodingException {
        if (relative == null) {
            return (new URI(firstPart)).normalize();
        }

        if (!relative.startsWith("/")) {
            relative = "/" + relative;
        }

        String protocol = "http";
        String host = firstPart.substring(7);
        if (firstPart.contains("https")) {
            protocol = "https";
            host = firstPart.substring(8);
        }

        return (new URI(protocol, host, relative, null)).normalize();
    }
}