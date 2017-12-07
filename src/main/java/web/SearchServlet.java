package web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import search.Document;
import search.Search;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class SearchServlet extends HttpServlet {
    private static final Search SEARCH = new Search();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE_GET_CONTENT =
            new TypeToken<Map<String, String>>() {}.getType();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        final Optional<String> json = request.getReader()
                .lines().peek(System.out::println).findFirst();
        System.out.println(json);
        if (json.isPresent() && !json.get().isEmpty()) {
            final Map<String, String> getContent = GSON.fromJson(json.get(), TYPE_GET_CONTENT);
            if (getContent.containsKey("query")) {
                final String query = getContent.get("query");

                final List<Document> documents = SEARCH.process(query);
                final List<Document> firstPage = new ArrayList<>();
                for (int i = 0; i < Math.min(documents.size(), 10); i++) {
                    firstPage.add(documents.get(i));
                }
                firstPage.forEach(System.out::println);
                GSON.toJson(firstPage, response.getWriter());
            }
        }
    }
}
