import crawl.Crawler;
import org.apache.log4j.PropertyConfigurator;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        Crawler crawler = new Crawler(10);
        crawler.start();

//        Thread.sleep(10000);
//        crawler.stop();
    }
}
