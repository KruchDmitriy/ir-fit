package db;

import db.readConfig.Config;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * Created by kate on 12.10.17.
 */

public class DbConnection {

    private static final Logger LOGGER = Logger.getLogger(DbConnection.class);

    private final Config config;
    private Connection connection;

    public DbConnection(@NotNull String pathToConfig) {
        this.config = new Config(pathToConfig);
        if (!createConnection()) {
            throw new RuntimeException("connection is not created");
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
                connection = DriverManager.getConnection(config.getConnection(), config.getUser(), config.getPassword());
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
                .append("SELECT * FROM ULR WHERE ")
                .append(" URL = ").append(url)
                .append(pathToSource == null ? "" : "and path_to_source = " + pathToSource);
        LOGGER.debug(sql.toString());
        try {

            final ResultSet res = runExecuteSqlSelectQeury(sql.toString());
            return res;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return null;
        }
    }

    public boolean insertToUrlRow(@NotNull String url, @NotNull String parentUrl,
                                  @NotNull String source, @Nullable String timeDownloading) {

        String sqlInsertToURL = " insert into URL " +
                "(url, source, source_hash, time_downloading) " +
                "values (?, ?, ?, ? ); ";

        String sqlInsertParent = "insert into parrent_url (url_id, parrent_id) values (?, ?); ";

        try (PreparedStatement statement = connection.prepareStatement(sqlInsertToURL)) {
            statement.setString(1, url);
            statement.setString(2, source);
            statement.setInt(3, source.hashCode());
            statement.setString(4, timeDownloading);

            final int rows = statement.executeUpdate();
            if (rows > 0) {
                final int urlId = statement.getGeneratedKeys().getInt(1);
                final ResultSet resultSet = selectFromUrlByUrl(parentUrl, null);
                if (resultSet == null) {
                    LOGGER.fatal(String.format("Parent URL {%s} is not found", parentUrl));
                    return false;
                }
                final int parentId = resultSet.getInt(1);

                try (PreparedStatement statement1 = connection.prepareStatement(sqlInsertParent)) {
                    statement1.setInt(1, urlId );
                    statement1.setInt(2, parentId);
                    statement1.executeUpdate();
                } catch (SQLException ex) {
                    LOGGER.debug(ex.getMessage());
                    LOGGER.fatal("FAIL sql = " + sqlInsertParent);
                    connection.rollback();
                    return false;
                }

            } else {
                connection.rollback();
                return  false;
            }

            connection.commit();
            return  true;

        } catch (SQLException ex) {
            LOGGER.debug(ex.getMessage());
            LOGGER.fatal("FAIL sql = " + sqlInsertToURL);
            return false;
        }
    }


    private boolean createURLTable() {

        StringBuilder sql = new StringBuilder("CREATE TABLE URL (")
                .append(" id serial PRIMARY KEY,\n")
                .append(" url VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" created_on TIMESTAMP  DEFAULT NOW(),\n")
                .append(" last_update TIMESTAMP   DEFAULT NOW(),\n")
                .append(" source VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" source_hash Integer NOT NULL, \n ")
                .append(" time_downloading TIMESTAMP NULL")
                .append(");");
        try {
            runExecuteSqlCreateQeury(sql.toString());
            LOGGER.debug("SECCES : create UTL table");
            return true;

        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
        }
        return false;
    }

    private boolean createParentTable() {
        StringBuilder sql = new StringBuilder()
                .append("create table parrent_url (")
                .append(" id serial PRIMARY KEY, \n ")
                .append(" url_id integer REFERENCES url (id), \n")
                .append(" parrent_id integer REFERENCES url(id) \n")
                .append(");");
        try {
            runExecuteSqlCreateQeury(sql.toString());
            LOGGER.debug("SUCCES : create parent_url table");
            return true;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
        }
        return false;

    }

    private void runExecuteSqlCreateQeury(@NotNull String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            connection.commit();
        }
    }

    @Nullable
    private ResultSet runExecuteSqlSelectQeury(@NotNull String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            return stmt.executeQuery();
        }
    }

    public static void main(String[] args) {
        String log4jConfPath = "src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        String pathToConfig = "src/main/resources/db.cfg";
        Path currentRelativePath = Paths.get(pathToConfig);
        String s = currentRelativePath.toAbsolutePath().toString();
        DbConnection connection = new DbConnection(s);
    }

}
