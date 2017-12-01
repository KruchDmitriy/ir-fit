package features;

import features.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class FindAddress {
    private static final String pathToJsonWithAddress = "./src/main/resources/address.json";
    private static final String pathToDirWithAddress = "./src/main/resources/address/";
    private ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>>
            idDocumentToListAddres = new ConcurrentHashMap<>();

    /**
     * "address(.+)<br\\s+/>";
     * "<p>Адрес:\K(.*)(?=</p>)" - dance line
     * "address\">\K(.*)(?=\<)"
     */

    public void initIdxDocument() throws IOException {
        Utils.loadArrayWithNameFiles();
        Utils.getNameDocumentToIndex().forEach((nameDoc, idx) ->
                idDocumentToListAddres.put(idx, new ConcurrentSkipListSet<>()));
    }

    public void setAddressFromDocument(@NotNull final Path pathToDocument) {
        try {
//            String str = "http:__belgorod.kartasporta" +
//                    ".ru_sport_borba_greko_rimskaya_sport_shashki_:\\\">Белгородская область, г." +
//                    " Старый Оскол, Олимпийс\n" +
//                    "кий мкр., д. 34";
//            String[] strArr = str.split(":\\\\\">", 2);
            Files.readAllLines(pathToDocument).parallelStream()
                    .filter(Objects::nonNull)
                    .map(str -> str.split("--->>>", 2))
                    .filter(Objects::nonNull)
                    .forEach(strings -> {
                        if (strings.length == 2 &&
                                strings[0] != null && !strings[0].isEmpty() &&
                                strings[1] != null && !strings[1].isEmpty()) {
                            Integer idx = Utils.getNameDocumentToIndex().get
                                    (strings[0]);
                            if (idx != null) {
                                idDocumentToListAddres.get(idx).add(strings[1]);
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJsonAddress() {
        Utils.dumpStructureToJson(idDocumentToListAddres, pathToJsonWithAddress);
    }

    public void readAllFilesWithAddress() {
        List<Path> addresses = data_preprocess.utils.Utils.getAllFiles(Paths.get
                (pathToDirWithAddress));
        addresses.parallelStream().filter(Objects::nonNull).forEach
                (addr -> {
                    System.out.println(addr);
                    setAddressFromDocument(addr);
                });
    }

    public static void main(String[] args) throws IOException {
        FindAddress findAddress = new FindAddress();
        findAddress.initIdxDocument();
        findAddress.readAllFilesWithAddress();
        findAddress.saveJsonAddress();
    }
}


