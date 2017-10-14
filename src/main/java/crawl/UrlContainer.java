package crawl;

import java.net.URI;
import java.net.URL;
import java.util.Date;

public interface UrlContainer {
    Page getUrl();
    void addUrl(Page page);

    class Page {
        private URI url;
        private URI baseUrl;
        private Date uploadDate;

        Page(URI url, URI baseUrl, Date uploadDate) {
            this.url = url;
            this.baseUrl = baseUrl;
            this.uploadDate = uploadDate;
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
    }
}
