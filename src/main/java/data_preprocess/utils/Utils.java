package data_preprocess.utils;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by kate on 26.10.17.
 */
public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    public static List<Path> getAllFiles(final @NotNull String directory) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            return paths.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException ex) {
            LOGGER.debug(String.format(" in dir %s exception messange: %s",
                    directory, ex.getMessage()));
            return Collections.emptyList();
        }
    }
}
