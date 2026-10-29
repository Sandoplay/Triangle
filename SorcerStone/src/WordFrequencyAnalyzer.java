import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Counts English words in an ASCII-compatible byte stream (Windows-1252, UTF-8, ...);
 * every byte that is not an ASCII letter, including all non-ASCII bytes, is a separator.
 */
public final class WordFrequencyAnalyzer {
    private static final int BUFFER_SIZE = 8192;

    private WordFrequencyAnalyzer() {
    }

    public record WordFrequency(String word, int count) {
    }

    /** Reads in fixed-size chunks without closing the caller-owned stream. */
    public static WordCounts countWords(InputStream input) throws IOException {
        WordCounts counts = new WordCounts();
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] word = new byte[32];
        int length = 0;
        int hash = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                // Maps 'A'..'Z' to 'a'..'z'; no other byte lands in 'a'..'z'.
                int lower = buffer[i] | 0x20;
                if (lower >= 'a' && lower <= 'z') {
                    if (length == word.length) {
                        word = Arrays.copyOf(word, length * 2);
                    }
                    word[length++] = (byte) lower;
                    hash = 31 * hash + lower;
                } else if (length > 0) {
                    counts.increment(word, length, hash);
                    length = 0;
                    hash = 0;
                }
            }
        }
        if (length > 0) {
            counts.increment(word, length, hash);
        }
        return counts;
    }

    /** Orders by descending frequency, then alphabetically for reproducible ties. */
    public static List<WordFrequency> topWords(WordCounts counts, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }
        // Keeps only the best `limit` word indexes, sorted, instead of sorting every word.
        int[] top = new int[Math.min(limit, counts.size())];
        int filled = 0;
        for (int index = 0; index < counts.size(); index++) {
            if (filled == top.length && !counts.ranksBefore(index, top[filled - 1])) {
                continue;
            }
            int position = insertionPoint(counts, top, filled, index);
            int moved = (filled < top.length ? filled++ : filled - 1) - position;
            System.arraycopy(top, position, top, position + 1, moved);
            top[position] = index;
        }
        WordFrequency[] result = new WordFrequency[filled];
        for (int i = 0; i < filled; i++) {
            result[i] = new WordFrequency(counts.word(top[i]), counts.count(top[i]));
        }
        return List.of(result);
    }

    private static int insertionPoint(WordCounts counts, int[] top, int filled, int index) {
        int low = 0;
        int high = filled;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (counts.ranksBefore(index, top[middle])) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }
}
