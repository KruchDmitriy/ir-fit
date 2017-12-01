package features;

import features.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class FIndAddress {

    private static final String pathToNameDocument =
            "../../../index_files.json";
    private static final String pathToDocuments = "../../../documents/";

    private static final String pathToFileWithAddressAfterGrep =
            "../../../address.txt";

    private ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>>
            idDocumentToListAddres = new ConcurrentHashMap<>();

    //    "address(.+)<br\\s+/>";
    //    "<p>Адрес:\K(.*)(?=</p>)" - dance line
    //    "address\">\K(.*)(?=\<)"


    public void initIdxDocument() {
        Utils.loadArrayWithNameFiles();
        Utils.getNameDocumentToIndex().forEach((nameDoc, idx) ->
                idDocumentToListAddres.put(idx, new ConcurrentSkipListSet<>()));
    }

    public void setAddressFromDocument(@NotNull final String pathToDocument) {
        try {
            Path path = Paths.get(pathToDocument);
            Files.readAllLines(path).parallelStream()
                    .map(str -> str.split(":\">", 2))
                    .forEach(strings -> {
                        if (strings.length >= 2) {
                            int idx = Utils.getNameDocumentToIndex().get
                                    (strings[0]);
                            idDocumentToListAddres.get(idx).add(strings[1]);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


