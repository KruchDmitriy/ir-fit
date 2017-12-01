package web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@WebServlet(urlPatterns = {"/search"})
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        Files.lines(Paths.get("src/main/web-app/WEB-INF/mapYandex.html"))
                .forEach(response.getWriter()::println);
    }
}
