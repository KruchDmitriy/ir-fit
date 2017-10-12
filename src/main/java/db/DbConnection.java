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

    private Config config;
    private Connection connection;

    public DbConnection(@NotNull String pathToConfig) {
        this.config = new Config(pathToConfig);
        if (!createConnection()) {
            throw new RuntimeException("connection is not created");
        }
        createURLTable();
        createParentTable();
    }

    public Connection getConnection() {
        return connection;
    }

    private boolean createConnection() {
        try {
            if (config.getDriver() != null) {
                Class.forName(config.getDriver());
                connection = DriverManager.getConnection(config.getConnection(), config.getUser(), config.getPassword());
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
                .append( " URL = ").append(url)
                .append( pathToSource == null ? "" :"path_to_source = " + pathToSource);
        LOGGER.debug(sql.toString());
        try {

            final ResultSet res = runExecuteSqlSelectQeury(sql.toString());
            return res;
        } catch (SQLException ex) {
            LOGGER.warn(ex.getMessage());
            return null;
        }
    }

    public boolean insertToUrlRow(@NotNull String url, @NotNull String parent,
                                  @NotNull String pathToSource) {
        return false;
    }


    private boolean createURLTable() {

        StringBuilder sql = new StringBuilder("CREATE TABLE URL (")
                .append(" id serial PRIMARY KEY,\n")
                .append(" url VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" created_on TIMESTAMP  DEFAULT NOW(),\n")
                .append(" last_update TIMESTAMP   DEFAULT NOW(),\n")
                .append(" path_to_source VARCHAR (10000) UNIQUE NOT NULL,\n")
                .append(" last_login TIMESTAMP")
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

    private void runExecuteSqlCreateQeury(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute(sql);
        }
    }

    private ResultSet runExecuteSqlSelectQeury(String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            return stmt.executeQuery();
        }
    }

    public static void main(String[] args) {
        String log4jConfPath = "src/main/resource/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        String pathToConfig = "src/main/resource/db.cfg";
        Path currentRelativePath = Paths.get(pathToConfig);
        String s = currentRelativePath.toAbsolutePath().toString();
        DbConnection connection = new DbConnection(s);
    }

}
