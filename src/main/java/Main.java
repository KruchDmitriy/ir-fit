import crawl.Crawler;
import org.apache.log4j.PropertyConfigurator;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        boolean loadFromDump = true;
        if (args.length != 0 &&
                ( args[0].equalsIgnoreCase("true") || args[0].equalsIgnoreCase("false"))) {
            loadFromDump = Boolean.valueOf(args[0]);
        }

        Crawler crawler = new Crawler(10, loadFromDump);
        crawler.start();
//        Thread.sleep(10000);
//        crawler.stop();
    }
}
