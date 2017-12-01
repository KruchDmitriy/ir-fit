package web;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SearchServer extends AbstractHandler {
    private static final int DEFAULT_PORT = 7000;

    public static void main(String[] args) throws Exception {
        Server server = new Server(DEFAULT_PORT);

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase("src/main/web-app");
//        context.setResourceBase(System.getProperty("./src/web-app"));
        server.setHandler(context);

        // Add dump servlet
        context.addServlet(SearchServlet.class, "/search");
        // Add default servlet
        context.addServlet(DefaultServlet.class, "/");

        server.start();
        System.out.println("Started!");
        server.dump(System.err);
        server.join();
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        request.setHandled(true);

        try (Stream<String> stream = Files.lines(
                Paths.get("./src/main/web-app/helloWord.html"))) {

//            stream.forEach(System.out::println);
            stream.forEach(response.getWriter()::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
