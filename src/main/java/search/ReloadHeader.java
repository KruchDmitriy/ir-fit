package search;

import crawl.NotValidUploadedException;
import crawl.Page;
import data_preprocess.utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ReloadHeader {

    private static final String documents = "../../../documents/";
    private static final String titles = "../../../titles/";

    public static void main(String[] args) {
        List<Path> docs = Utils.getAllFiles(Paths.get(documents));

        docs.stream().parallel().forEach(doc -> {
            String fileName = doc.getFileName().toString();
            System.out.println("parse : " + fileName);
            String url = fileName.replaceAll("_", "/");
            try {
                Page page = new Page(new URL(url), null);
                String title = page.getHead().getElementsByTag("title").text();
                Files.write(Paths.get(titles + fileName), title.getBytes(),
                        StandardOpenOption.CREATE_NEW);

            } catch (URISyntaxException | MalformedURLException
                    | NotValidUploadedException e) {
                System.out.println("        skip file  :" + fileName);
            } catch (IOException e) {
                System.out.println("         wrong in write to file " + titles + fileName);
            }
        });
    }

}
