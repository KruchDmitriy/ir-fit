package data_preprocess;

import data_preprocess.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import util.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class HTML2Text {
    public static void main(String[] args) {
        Utils.getAllFiles(Config.DOCUMENTS_PATH)
                .parallelStream()
                .forEach(file -> {
                    try {
                        final String content = Files.lines(file)
                                .collect(Collectors.joining("\n"));
                        String fileName = file.getName(file.getNameCount() - 1).toString();
                        Path newFile = Paths.get(Config.TEXTS_DIR + fileName);

                        String text = Jsoup.parse(content).text().toLowerCase();
                        text = processWord(text);

                        Files.write(newFile, text.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static String processWord(final @NotNull String str) {
        return str.replaceAll("\\p{Punct}+", "");
    }
}
