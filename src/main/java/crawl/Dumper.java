package crawl;

public class Dumper extends Thread {
    private Runnable dumpFunction;

    Dumper(Runnable dumpFunction) {
        this.setDaemon(true);
        this.dumpFunction = dumpFunction;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5 *   // minutes to sleep
                    60 *   // seconds to a minute
                    1000); // milliseconds to a second
            dumpFunction.run();
        } catch (InterruptedException e) {
            dumpFunction.run();
        }
    }
}
