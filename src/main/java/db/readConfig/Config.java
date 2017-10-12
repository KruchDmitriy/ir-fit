package db.readConfig;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by kate on 12.10.17.
 */
public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class);

    private String driver;
    private String user;
    private String password;
    private String connection;

    public Config(final String pathToConfig) {
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

    public String getDriver() {
        return driver;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

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
