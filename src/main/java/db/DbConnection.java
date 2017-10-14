package db;

import com.sun.deploy.security.ValidationState;
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
    private static final String PATH = PATH_TO_RESOURCES + "documents/";
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
        createURLTable();
        createParentTable();
    }

    @NotNull
    public Connection getConnection() {
        return connection;
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

    @Nullable
    public ResultSet selectFromUrlByUrl(@NotNull String url, @Nullable String pathToSource) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT * FROM url_dt WHERE ")
                .append(" URL like ").append("'" + url + "'")
                .append(pathToSource == null ? "" : "and path_to_source = " + pathToSource);
        LOGGER.debug(sql.toString());
        try {
            return runExecuteSqlSelectQeury(sql.toString());
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return null;
        }
    }

    public boolean insertToUrlRow(@NotNull Page page) {

        String sqlInsertToURL = " insert into url_dt " +
                "(url, source, source_hash, time_downloading) " +
                "values (?, ?, ?, ? ) ";

        String sqlInsertParent = "insert into parent_url (url_id, parent_id) " +
                "values (?, ?) ";

        final String pathToFile = PATH + page.getUrl().toString()
                .replaceAll("/", "_");

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(pathToFile)))) {
            writer.write(page.getBody());
        } catch (IOException ex) {
            LOGGER.warn(ex.getMessage());
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(sqlInsertToURL,
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, page.getUrl().toString());
            statement.setString(2, pathToFile);
            statement.setInt(3, page.hashCode());
            statement.setString(4, page.getCreateDate().toString());

            final int rows = statement.executeUpdate();
            if (rows > 0) {
                ResultSet resultSet = statement.getGeneratedKeys();
                if (resultSet == null || !resultSet.next()) {
                    throw new SQLException("result set is null");

                }
                final int urlId = resultSet.getInt(1);
//                int rowsParent =
                resultSet = selectFromUrlByUrl(page.getParentUrl().toString(),
                        null);
//                if (rowsParent > 0 ) {
//                        resultSet = statement.getGeneratedKeys();
                if (resultSet == null || !resultSet.next()) {
                    throw new SQLException("result set is null");
                }
                final int parentId = resultSet.getInt(1);

                try (PreparedStatement statement1 =
                             connection.prepareStatement(sqlInsertParent)) {
                    statement1.setInt(1, urlId);
                    statement1.setInt(2, parentId);
                    statement1.executeUpdate();
                } catch (SQLException ex) {
                    throw new SQLException(ex);
                }
//                } else  {
//                    throw new SQLException("0 rows update");
//                }

            } else {
                throw new SQLException("0 rows update");
            }

            connection.commit();
            return true;

        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.info("rollback fail");
            }
            LOGGER.debug(ex.getMessage());
            LOGGER.fatal("FAIL sql = " + sqlInsertToURL);
            return false;
        }
    }

    private boolean createURLTable() {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS url_dt (")
                .append(" id serial PRIMARY KEY,\n")
                .append(" url VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" created_on TIMESTAMP  DEFAULT NOW(),\n")
                .append(" last_update TIMESTAMP   DEFAULT NOW(),\n")
                .append(" source VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" source_hash Integer NOT NULL, \n ")
                .append(" time_downloading TIMESTAMP NULL")
                .append(");");
        try {
            runExecuteSqlQuery(sql.toString());
            LOGGER.debug("SUCCESS : create url table");
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

    private boolean createParentTable() {
        StringBuilder sql = new StringBuilder()
                .append(" create table IF NOT EXISTS parent_url (")
                .append(" id serial PRIMARY KEY, \n ")
                .append(" url_id integer REFERENCES url (id), \n")
                .append(" parent_id integer REFERENCES url(id) \n")
                .append(");");
        try {
            runExecuteSqlQuery(sql.toString());
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

    @Nullable
    private ResultSet runExecuteSqlSelectQeury(@NotNull String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
//            if (statement.execute(sql)) {
//                return null;
//            }
            return statement.executeQuery(sql);
        }
    }
}
