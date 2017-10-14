package crawl;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Page {
    private final URI url;
    private final URI parentUrl;
    private Date uploadDate;
    private Element body;
    private static final Logger LOGGER = Logger.getLogger(Page.class);

    Page(URI url, URI baseUrl) {
        this.url = url;
        this.parentUrl = baseUrl;
    }

    public URI getUrl() {
        return url;
    }

    public URI getParentUrl() {
        return parentUrl;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public String getBody() {
        if (body == null) {
            downloadPage();
        }

        return body.html();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public List<Page> expandPage() {
        downloadPage();

        return body.getElementsByTag("a")
                .stream()
                .map(tag -> tag.attr("href"))
                .filter(Page::checkExtension)
                .map(link -> {
                    try {
                        return new Page(new URI(link), url);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void downloadPage() {
        Document document;
        try {
            Connection.Response response = Crawler.getConnection(url.toString()).execute();
            uploadDate = Calendar.getInstance().getTime();
            document = response.parse();
        } catch (IOException e) {
            LOGGER.error("Error while downloading page " + url.toString(), e);
            throw new RuntimeException(e);
        }

        body = document.body();
    }

    private static boolean checkExtension(String uri) {
        String extension = uri.substring(uri.lastIndexOf(".") + 1);
        return extension.equals("html");
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

        return body.toString().hashCode();
    }
}