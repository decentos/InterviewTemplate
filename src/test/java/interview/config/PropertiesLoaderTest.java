package interview.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesLoaderTest {
    private static String test;

    @BeforeAll
    static void setup() {
        Properties config = PropertiesLoaderTest.loadProperties("application.properties");
        test = config.getProperty("test");
    }

    @Test
    void test() {
        assertEquals("key example", test);
        assertNotEquals("not equals", test);
        assertTrue(Boolean.TRUE);
        assertFalse(Boolean.FALSE);

        Throwable exception = assertThrows(UnsupportedOperationException.class, () -> {
            throw new UnsupportedOperationException("Not supported");
        });
        assertEquals("Not supported", exception.getMessage());

        int[] numbers = {0, 1, 2, 3, 4};
        assertAll("numbers",
                () -> assertNotEquals(numbers[0], 1),
                () -> assertEquals(numbers[3], 3),
                () -> assertEquals(numbers[1], 1)
        );
    }

    private static Properties loadProperties(String resourceFileName) {
        Properties configuration = new Properties();
        try (InputStream inputStream = PropertiesLoaderTest.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            configuration.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error retrieving configuration: %s", resourceFileName));
        }
        return configuration;
    }
}
