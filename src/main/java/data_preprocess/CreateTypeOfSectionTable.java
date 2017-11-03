package data_preprocess;

import data_preprocess.utils.Utils;
import db.DbConnection;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CreateTypeOfSectionTable {

    private static final Logger LOGGER = Logger.getLogger(CreateTypeOfSectionTable.class);
    private static final String TYPE_SPORT = "src/main/resources/type_sport.txt";
    private final DbConnection connection;
    private List<String> section;

    public CreateTypeOfSectionTable() {
        section = new ArrayList<>();
        connection = new DbConnection();
        connection.createTableForSection();
    }

    public void createTypeOfSectionTable() {
        try {
            Stream<String> lines = Files.lines((Paths.get(TYPE_SPORT)));
            lines.forEach(connection::insertTypeSection);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void initListSection() {
        try {
            Files.lines(Paths.get(TYPE_SPORT))
                    .forEach(section::add);
        } catch (IOException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    public void parseText() {
        for (Path path : Utils.getAllFiles(Paths.get("../ir-fit/text"))) {
            try {
                Stream<String> lines = Files.lines((Paths.get(TYPE_SPORT)));
                lines.forEach(line -> {
                    for (String typeSection : section) {
                        if (line.contains(typeSection)) {
                            connection.insertTypeSectionForUrl(typeSection, path.toString());
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        CreateTypeOfSectionTable createTypeOfSectionTable = new CreateTypeOfSectionTable();
        createTypeOfSectionTable.createTypeOfSectionTable();
        createTypeOfSectionTable.initListSection();
        createTypeOfSectionTable.parseText();
    }

}
