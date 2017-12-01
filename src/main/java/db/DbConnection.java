package db;

import crawl.NotValidUploadedException;
import crawl.Page;
import db.readConfig.Config;
import features.utils.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class DbConnection {
    private static final Logger LOGGER = Logger.getLogger(DbConnection.class);
    private static final String PATH_TO_RESOURCES = "src/main/resources/";
    private static final String PATH_TO_CONFIG = PATH_TO_RESOURCES + "db.cfg";

    private final Config config;
    private Connection connection;


    public DbConnection() {
        this(PATH_TO_CONFIG);
    }

    public DbConnection(@NotNull String pathToConfig) {
        config = new Config(pathToConfig);
        if (!createConnection()) {
            throw new RuntimeException("Database connection is not created");
        }
    }

    private void createTables() {
        createURLTable();
        createParentTable();
        createProcedureGetId();
    }

    public void createTableForSection() {
        createTypeSectionTable();
        createUrlToSectionTable();
    }

    public static void main(String[] args) {
        DbConnection connection = new DbConnection();
        connection.createTables();
    }

    private boolean createConnection() {
        if (connection != null) {
            return true;
        }
        try {
            if (config.getDriver() != null) {
                Class.forName(config.getDriver());
                connection = DriverManager.getConnection(config.getConnection(),
                        config.getUser(), config.getPassword());
                connection.setAutoCommit(false); //only transaction
            } else {
                return false;
            }
        } catch (ClassNotFoundException ex) {
            LOGGER.fatal(String.format(" not load driver %s", config.getDriver()));
            return false;
        } catch (SQLException ex) {
            LOGGER.fatal(String.format(" connection fail %s", config.getConnection()));
            return false;
        }

        LOGGER.debug("Create db connection");
        return true;
    }

    public ConcurrentHashMap<Integer, String> readAllTableUrls() throws IOException {
        if (!createConnection()) {
            return null;
        }

        Utils.loadArrayWithNameFiles(); // name with _ to idx

        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select * from urls");

            while (rs.next()) {
                String originUrl = rs.getString(2);
                String fileNameInSystem = originUrl.replaceAll("/", "_");
                Integer idx = Utils.getNameDocumentToIndex()
                        .get(fileNameInSystem);
                if (idx != null) {
                    map.put(idx, originUrl);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }


    int selectFromUrlByUrl(@NotNull String url) {
        String sql = " { ? = call getUrlId ( ? ) } ";
        try {
            return runExecuteSqlSelectQuery(sql, url);
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return -1;
        }
    }

    int selectFromUrlByPath(@NotNull String pathToFile) {
        String sql = " { ? = call getUrlIdByFile ( ? ) } ";
        try {
            return runExecuteSqlSelectQuery(sql, pathToFile);
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return -1;
        }
    }

    int selectFromTypeSectionByNameSection(@NotNull String sectionName) {
        String sql = " { ? = call getSectionId ( ? ) } ";
        try {
            return runExecuteSqlSelectQuery(sql, sectionName);
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return -1;
        }
    }

    public boolean insertTypeSection(@NotNull String typeSport) {
        String sql = " insert into type_of_section " +
                "(name_section) " +
                "values (?) ";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, typeSport);
            final int rows = statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LOGGER.debug(sql + " ERROR ");
        }
        return false;
    }

    public void insertTypeSectionForUrl(@NotNull String pathToFile, @NotNull String section) {
        int idSection = selectFromTypeSectionByNameSection(section);
        int idUrl = selectFromUrlByPath(pathToFile);

        String sql = " insert into url_to_section " +
                "(id_url, id_section) " +
                "values (?, ? ) ";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUrl);
            statement.setInt(2, idSection);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.debug(sql + " ERROR ");
        }

    }

    public boolean insertToUrlRow(@NotNull Page page, @NotNull String pathToFile) throws NotValidUploadedException {
        String sqlInsertToURL = " insert into urls " +
                "(url, source, source_hash, time_downloading) " +
                "values (?, ?, ?, ? ) ";

        String sqlInsertParent = "insert into parent_url (url_id, parent_id) " +
                "values (?, ?) ";

        try (PreparedStatement statement =
                     connection.prepareStatement(sqlInsertToURL,
                             Statement.RETURN_GENERATED_KEYS);
             PreparedStatement statement1 =
                     connection.prepareStatement(sqlInsertParent)) {
            statement.setString(1, page.getUrl().toString());
            statement.setString(2, pathToFile);
            statement.setString(3, page.hash());
            statement.setString(4, page.getCreationDate().toString());

            final int rows = statement.executeUpdate();
            if (rows > 0) {
                ResultSet resultSet = statement.getGeneratedKeys();
                if (resultSet == null || !resultSet.next()) {
                    throw new SQLException("result set is null");
                }
                final int urlId = resultSet.getInt(1);
                int parentId = selectFromUrlByUrl(page.getParentUrl().toString());
                if (parentId < 0) {
                    throw new SQLException();
                }
                statement1.setInt(1, urlId);
                statement1.setInt(2, parentId);
                statement1.executeUpdate();

            } else {
                throw new SQLException("0 rows update");
            }

            connection.commit();
            return true;

        } catch (SQLException ex) {
            LOGGER.warn(ex.toString());
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.error("rollback fail");
            }
            LOGGER.debug(ex.getMessage());
            return false;
        }
    }

    private boolean createURLTable() {
        String createUrl = "CREATE TABLE IF NOT EXISTS urls (" +
                " id serial PRIMARY KEY,\n" +
                " url VARCHAR (10000) UNIQUE NOT NULL,\n" +
                " created_on TIMESTAMP  DEFAULT NOW(),\n" +
                " last_update TIMESTAMP   DEFAULT NOW(),\n" +
                " source VARCHAR (10000) UNIQUE NOT NULL,\n" +
                " source_hash VARCHAR(10000) UNIQUE NOT NULL, \n " +
                " time_downloading VARCHAR(100) NULL" +
                ");";

        String insertBaseUrl = "INSERT INTO urls " +
                "(url, source, source_hash) " +
                "values ('http://www.google.com/', 'www.google.com', '0')";
        try {
            runExecuteSqlQuery(createUrl);
            runExecuteSqlQuery(insertBaseUrl);
            LOGGER.debug("SUCCESS : create url table");
            return true;

        } catch (SQLException e) {
            LOGGER.warn(e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                LOGGER.info("rollback error");
            }
        }
        return false;
    }

    private void createProcedureGetId() {
        String query = "CREATE OR REPLACE FUNCTION getUrlId(curUrl VARCHAR) RETURNS INT\n" +
                "AS $getUrlId$ DECLARE result INT;\n" +
                "BEGIN\n" +
                "  SELECT id INTO result FROM urls WHERE url = curUrl;\n" +
                "  RETURN result;\n" +
                "END;\n" +
                "$getUrlId$ LANGUAGE plpgsql;";
        try {
            runExecuteSqlQuery(query);
            LOGGER.debug("SUCCESS: create getId procedure");
        } catch (SQLException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    private boolean createParentTable() {
        String sql = " create table IF NOT EXISTS parent_url (" +
                " id serial PRIMARY KEY, \n " +
                " url_id integer REFERENCES urls(id), \n" +
                " parent_id integer REFERENCES urls(id) \n" +
                ");";
        try {
            runExecuteSqlQuery(sql);
            LOGGER.debug("SUCCESS : create parent_url table");
            return true;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.info("rollback error");
            }
        }

        return false;
    }


    private boolean createTypeSectionTable() {
        String sql = " create table IF NOT EXISTS type_of_section (" +
                " id serial PRIMARY KEY, \n " +
                " name_section VARCHAR (10000) UNIQUE NOT NULL, \n" +
                ");";
        try {
            runExecuteSqlQuery(sql);
            LOGGER.debug("SUCCESS : create type_of_section table");
            return true;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.info("rollback error");
            }
        }
        return false;
    }

    private boolean createUrlToSectionTable() {
        String sql = " create table IF NOT EXISTS url_to_section (" +
                " id serial PRIMARY KEY, \n " +
                " id)url integer REFERENCES urls(id), \n" +
                " id_section integer REFERENCES type_of_section(id) \n" +
                ");";
        try {
            runExecuteSqlQuery(sql);
            LOGGER.debug("SUCCESS : create url_to_section table");
            return true;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.info("rollback error");
            }
        }
        return false;
    }


    private void runExecuteSqlQuery(@NotNull String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            connection.commit();
        }
    }

    private int runExecuteSqlSelectQuery(@NotNull String sql, @NotNull String url) throws SQLException {
        CallableStatement statement = connection.prepareCall(sql);
        statement.registerOutParameter(1, Types.INTEGER);
        statement.setString(2, url);
        statement.execute();
        return statement.getInt(1);
    }
}
