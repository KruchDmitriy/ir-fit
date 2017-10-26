package util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    public static final String DATA = "../ir-fit-data/";
    public static final String DOCUMENTS_DIR = DATA + "documents/";
    public static final String TEXTS_DIR = DATA + "texts/";

    public static final Path DOCUMENTS_PATH = Paths.get("../ir-fit-data/documents/")
            .toAbsolutePath().normalize();
}
