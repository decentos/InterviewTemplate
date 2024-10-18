package kaluza.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertiesLoader {
    private static final Logger log = Logger.getLogger(PropertiesLoader.class.getName());

    public static Properties loadProperties(String resourceFileName) {
        Properties configuration = new Properties();
        try (InputStream inputStream = PropertiesLoader.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            configuration.load(inputStream);
        } catch (Exception e) {
            log.warning(String.format("Error retrieving configuration: %s", resourceFileName));
            e.printStackTrace();
        }
        return configuration;
    }
}
