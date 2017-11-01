package crawl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Page {
    private static final Logger LOGGER = Logger.getLogger(Page.class);

    private final URL url;
    private final URL parentUrl;

    private final URI uri;

    private LocalDateTime creationDate;
    private Element head;
    private Element body;
    private boolean isValidUploaded = false;

    Page(URL url, URL baseUrl) throws URISyntaxException {
        this.url = url;
        this.parentUrl = baseUrl;
        this.uri = url.toURI();
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

    public String getBody() throws NotValidUploadedException {
        if (body == null) {
            downloadPage();
        }

        if (!isValidUploaded) {
            throw new NotValidUploadedException();
        }

        return body.html();
    }

    public Element getHead() throws NotValidUploadedException {
        if (head == null) {
            downloadPage();
        }

        if (!isValidUploaded) {
            throw new NotValidUploadedException();
        }

        return head;
    }

    public String getText() throws NotValidUploadedException {
        return Jsoup.parse(getBody()).text();
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
        return (obj instanceof Page) && ((Page) obj).getUrl().toString()
                .equals(url.toString());
    }

    @Override
    public int hashCode() {
        return url.toString().hashCode();
    }

    public String hash() throws NotValidUploadedException {
        return DigestUtils.md5Hex(getText());
    }

    public URI getUri() {
        return uri;
    }

    private void downloadPage() {
        if (body != null) {
            return;
        }

        try {
            Connection.Response response = Crawler.getConnection(url.toString()).execute();
            creationDate = LocalDateTime.now();

            Document document = response.parse();
            head = document.head();
            body = document.body();
            isValidUploaded = true;
        } catch (IOException e) {
            LOGGER.error("Error while downloading page " + url.toString() + e);
            isValidUploaded = false;
        }
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