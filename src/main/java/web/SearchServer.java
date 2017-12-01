package web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class SearchServer {
    private static final int DEFAULT_PORT = 7000;

    public static void main(String[] args) throws Exception {
        Server server = new Server(DEFAULT_PORT);

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase("src/main/web-app");

        context.addServlet(DefaultServlet.class, "/");
//        context.addServlet(HomeServlet.class, "/home");
        context.addServlet(SearchServlet.class, "/search");
        server.setHandler(context);

        server.start();
        System.out.println("Started!");
        server.dump(System.err);
        server.join();
    }
}
