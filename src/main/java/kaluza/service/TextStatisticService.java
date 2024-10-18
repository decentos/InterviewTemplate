package kaluza.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextStatisticService {
    private final String text;
    private String[] sentences;
    private final List<String> words;
    private int maxLength;
    private final Map<String, Integer> wordsFreq;
    private List<String> popularWords;
    private final Map<String, Integer> phrasesFreq;
    private List<String> popularPhrases;

    public TextStatisticService(String text) {
        this.text = text;
        this.sentences = null;
        this.words = new ArrayList<>();
        this.maxLength = 0;
        this.wordsFreq = new HashMap<>();
        this.popularWords = null;
        this.phrasesFreq = new HashMap<>();
        this.popularPhrases = null;
    }

    public int getSentencesCount() {
        if (sentences == null) {
            parseSentences();
        }
        return sentences.length;
    }

    public int getWordsCount() {
        if (words.isEmpty()) {
            parseWords();
        }
        return words.size();
    }

    public int getLongestWordLength() {
        if (words.isEmpty()) {
            parseWords();
        }
        return maxLength;
    }

    public List<String> getPopularWords(int count) {
        if (words.isEmpty()) {
            parseWords();
        }
        return popularWords.subList(0, count);
    }

    public int getOnceOccurWordsPercentage() {
        if (words.isEmpty()) {
            parseWords();
        }

        int onceOccurCount = (int) wordsFreq.values().stream().filter(it -> it == 1).count();
        return onceOccurCount * 100 / wordsFreq.size();
    }

    public int getAverageWordsPerSentence() {
        if (words.isEmpty()) {
            parseWords();
        }

        int sentencesCount = getSentencesCount();
        int wordsCount = getWordsCount();

        return wordsCount / sentencesCount;
    }

    public List<String> getPopularPhrases(int count) {
        if (words.isEmpty()) {
            parseWords();
        }
        return popularPhrases.subList(0, count);
    }

    private void parseSentences() {
        sentences = text.split("\\.");
    }

    private void parseWords() {
        if (sentences == null) {
            parseSentences();
        }

        for (String sentence : sentences) {
            String[] currWords = sentence.split("\\s");

            for (String word : currWords) {
                maxLength = Math.max(maxLength, word.length());
                words.add(word);
                wordsFreq.merge(word, 1, Integer::sum);

                if (words.size() > 1) {
                    int lastIndex = words.size() - 1;
                    String phrase = String.format("%s %s", words.get(lastIndex - 1), words.get(lastIndex));
                    phrasesFreq.merge(phrase, 1, Integer::sum);
                }
            }
        }
        popularWords = getPopulars(wordsFreq);
        popularPhrases = getPopulars(phrasesFreq);
    }

    private List<String> getPopulars(Map<String, Integer> freq) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
