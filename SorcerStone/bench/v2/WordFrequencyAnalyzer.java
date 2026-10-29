package v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Counts English words; punctuation, digits and non-ASCII letters are separators. */
public final class WordFrequencyAnalyzer {
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+");
    private static final Comparator<WordFrequency> REPORT_ORDER =
            Comparator.comparingLong(WordFrequency::count).reversed()
                    .thenComparing(WordFrequency::word);

    private WordFrequencyAnalyzer() {
    }

    public record WordFrequency(String word, long count) {
    }

    /** Reads line by line without closing the caller-owned reader. */
    public static Map<String, Long> countWords(Reader input) throws IOException {
        BufferedReader reader = input instanceof BufferedReader buffered
                ? buffered : new BufferedReader(input);
        Map<String, Long> frequencies = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            var matcher = WORD.matcher(line);
            while (matcher.find()) {
                String word = matcher.group().toLowerCase(Locale.ROOT);
                frequencies.merge(word, 1L, Long::sum);
            }
        }
        return frequencies;
    }

    /** Orders by descending frequency, then alphabetically for reproducible ties. */
    public static List<WordFrequency> topWords(Map<String, Long> frequencies, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }
        return frequencies.entrySet().stream()
                .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                .sorted(REPORT_ORDER)
                .limit(limit)
                .toList();
    }
}
