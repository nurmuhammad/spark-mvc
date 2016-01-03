package com.sparkmvc.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author nurmuhammad
 *
 */

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static Config instance = new Config();
    private Properties properties;

    private Config() {
        logger.info("Init Config class file");
        properties = new Properties();
        try (final InputStream stream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(stream);
        } catch (Exception e) {
            logger.error("Can't read the config.properties file. Please check the file.", e);
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public static String get(String key){
        return getInstance().properties.getProperty(key);
    }

    public static String get(String key, String defaultValue){
        return getInstance().properties.getProperty(key, defaultValue);
    }

    public static int get(String key, int defaultValue){
        try {
            String toret = getInstance().properties.getProperty(key, String.valueOf(defaultValue));

            return Integer.valueOf(toret);
        } catch (NumberFormatException e) {
            logger.warn("Getting key by '"+key+"', throws exception. ", e);
            return defaultValue;
        }
    }

}
