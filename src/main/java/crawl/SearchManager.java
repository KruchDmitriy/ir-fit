package crawl;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class SearchManager {
    private final static List<String> allowedWords = new ArrayList<>();
    private final static String FILE_ALLOWED_WORDS = "src/main/recources/allowed_words.txt";
    private final static Logger LOGGER = Logger.getLogger(SearchManager.class);

    static {
        try {
            Files.lines(Paths.get(FILE_ALLOWED_WORDS))
                    .forEach(allowedWords::add);
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    static boolean isGoodPage(final Page page) throws NotValidUploadedException {
        for (String word: allowedWords) {
            if (page.getText().contains(word)) {
                return true;
            }
        }

        return false;
    }
}
