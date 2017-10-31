package data_preprocess.utils;

import org.apache.commons.lang3.CharSetUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    public static List<Path> getAllFiles(final @NotNull Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException ex) {
            LOGGER.debug(String.format(" in dir %s exception messange: %s",
                    directory, ex.getMessage()));
            return Collections.emptyList();
        }
    }

    public static Stream<String> readFiles(Path directory) {
        Charset charset = Charset.defaultCharset();
        final CharsetEncoder charsetEncoder = charset.newEncoder();
        charsetEncoder.onMalformedInput(CodingErrorAction.REPLACE);
        charsetEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        return getAllFiles(directory).stream()
                .flatMap(file -> {
//                    System.out.println(file);
                    try {
                        return Files.lines(file);
                    } catch (MalformedInputException ignored) {
                        System.out.println(Math.sin(10.));
                    }
                    catch (UncheckedIOException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        System.out.println(Math.sin(20.));
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull);
    }
}
