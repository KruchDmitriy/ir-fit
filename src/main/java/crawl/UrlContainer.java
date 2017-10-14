package crawl;

import org.jetbrains.annotations.NotNull;

public interface UrlContainer {
    Page getUrl();
    void addUrl(Page page);
    void dump(@NotNull String fileQueue, @NotNull String fileExistsUrl);
}
