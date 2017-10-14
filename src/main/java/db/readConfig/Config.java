package db.readConfig;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class);

    private String driver;
    private String user;
    private String password;
    private String connection;

    public Config(@NotNull final String pathToConfig) {
        FileInputStream config;
        try {
            config = new FileInputStream(pathToConfig);
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(String.format("File: %s not found", pathToConfig));
            throw  new RuntimeException();
        }

        Properties prop = new Properties();
        try {
            prop.load(config);
        } catch (IOException ex) {
            LOGGER.fatal("not valid properties file");
            throw new RuntimeException();
        }
        driver = prop.getProperty("jdbc.driver");
        password = prop.getProperty("jdbc.password");
        connection = prop.getProperty("jdbc.conection");
        user = prop.getProperty("jdbc.user");

        LOGGER.debug(" load config: " + this);
    }

    @Nullable
    public String getDriver() {
        return driver;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public String getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return "Config{" +
                "driver='" + driver + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", connection='" + connection + '\'' +
                '}';
    }
}
