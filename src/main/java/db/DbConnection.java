package db;

import crawl.NotValidUploadedException;
import crawl.Page;
import db.readConfig.Config;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.*;

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

    public static void main(String[] args) {
        DbConnection connection = new DbConnection();
        connection.createTables();
    }

    private boolean createConnection() {
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

    int selectFromUrlByUrl(@NotNull String url) {
        String sql = " { ? = call getUrlId ( ? ) } ";
        try {
            return runExecuteSqlSelectQuery(sql, url);
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return -1;
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

    private void runExecuteSqlQuery(@NotNull String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            connection.commit();
        }
    }

    private int runExecuteSqlSelectQuery(@NotNull String sql, @NotNull String url) throws SQLException {
        CallableStatement statement = connection.prepareCall(sql);
        statement.registerOutParameter(1, Types.INTEGER);
        statement.setString(2,  url);
        statement.execute();
        return statement.getInt(1);
    }
}
