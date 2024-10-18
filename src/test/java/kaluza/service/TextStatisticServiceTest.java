package kaluza.service;

import kaluza.config.PropertiesLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TextStatisticServiceTest {
    private static TextStatisticService statisticService;

    @BeforeAll
    static void setup() {
        Properties config = PropertiesLoader.loadProperties("application.properties");
        String text = config.getProperty("text");
        statisticService = new TextStatisticService(text);
    }

    @Test
    void getSentencesCount() {
        int expectedCount = 22;
        int actualCount = statisticService.getSentencesCount();
        assertEquals(expectedCount, actualCount);
    }

    @Test
    void getWordsCount() {
        int expectedCount = 260;
        int actualCount = statisticService.getWordsCount();
        assertEquals(expectedCount, actualCount);
    }

    @Test
    void getLongestWordLength() {
        int expectedLength = 15;
        int actualLength = statisticService.getLongestWordLength();
        assertEquals(expectedLength, actualLength);
    }

    @Test
    void getPopularWords() {
        List<String> expectedWords = List.of("non", "est", "enim", "quid", "ut", "mihi");
        List<String> actualWords = statisticService.getPopularWords(6);
        assertEquals(expectedWords, actualWords);
    }

    @Test
    void getOnceOccurWordsPercentage() {
        int expectedPercentage = 82;
        int actualPercentage = statisticService.getOnceOccurWordsPercentage();
        assertEquals(expectedPercentage, actualPercentage);
    }

    @Test
    void getAverageWordsPerSentence() {
        int expectedAverage = 11;
        int actualAverage = statisticService.getAverageWordsPerSentence();
        assertEquals(expectedAverage, actualAverage);
    }

    @Test
    void getPopularPhrases() {
        List<String> expectedPhrases = List.of("et mihi", "quoniam et", "lorem ipsum");
        List<String> actualPhrases = statisticService.getPopularPhrases(3);
        assertEquals(expectedPhrases, actualPhrases);
    }
}