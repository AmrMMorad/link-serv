package org.bibalex.linkserv.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesHandler {
    static Properties prop = new Properties();

    public static void initializeProperties(){
        try {
            InputStream input = PropertiesHandler.class.getClassLoader().getResourceAsStream("config.properties");
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key){
        return prop.getProperty(key);
    }
}
