import crawl.Crawler;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Crawler crawler = new Crawler(1);
        crawler.start();
        Thread.sleep(10000);
        crawler.stop();
    }
}
