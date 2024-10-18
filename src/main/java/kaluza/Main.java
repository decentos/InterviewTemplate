package kaluza;

import kaluza.config.PropertiesLoader;
import kaluza.service.TextStatisticService;

import java.util.Arrays;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties config = PropertiesLoader.loadProperties("application.properties");
        String text = config.getProperty("text");
        TextStatisticService statistic = new TextStatisticService(text);
        System.out.println(statistic.getSentencesCount());
        System.out.println(statistic.getWordsCount());
        System.out.println(statistic.getLongestWordLength());
        System.out.println(statistic.getPopularWords(6));
        System.out.println(statistic.getOnceOccurWordsPercentage());
        System.out.println(statistic.getAverageWordsPerSentence());
        System.out.println(statistic.getPopularPhrases(3));
    }
}
