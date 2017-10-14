package crawl;

import com.sun.jndi.toolkit.url.Uri;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class Page {
    private final URI url;
    private final URI baseUrl;
    private Date uploadDate;
    private static final Logger LOGGER = Logger.getLogger(Page.class);

    Page(URI url, URI baseUrl) {
        this.url = url;
        this.baseUrl = baseUrl;
    }

    public URI getUrl() {
        return url;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public List<Page> expandPage() {
        Document document;
        try {
            Connection.Response response = Crawler.getConnection(url.toString()).execute();
            uploadDate = Calendar.getInstance().getTime();
            document = response.parse();
        } catch (IOException e) {
            LOGGER.error("Error while downloading page " + url.toString(), e);
            throw new RuntimeException(e);
        }

        return document.getElementsByTag("a")
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

    private static boolean checkExtension(String uri) {
        String extension = uri.substring(uri.lastIndexOf(".") + 1);
        return extension.equals("html");
    }
}