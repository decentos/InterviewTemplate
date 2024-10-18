package interview.config;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    public static Properties loadProperties(String resourceFileName) {
        Properties configuration = new Properties();
        try (InputStream inputStream = PropertiesLoader.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            configuration.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error retrieving configuration: %s", resourceFileName));
        }
        return configuration;
    }
}
