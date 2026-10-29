import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dependency-free regression tests. Failures throw AssertionError without -ea. */
public final class WordFrequencyAnalyzerTest {
    private static int checks;

    public static void main(String[] args) throws IOException {
        equal(Map.of("harry", 3, "ron", 1), count("Harry HARRY harry Ron"), "case");
        equal(Map.of("hermione", 1, "he", 2), count("hermione he he"), "substrings");
        equal(Map.of(), count(""), "empty file");
        equal(Map.of(), count(" \n\t123!?Ж"), "no English words");
        equal(Map.of("harry", 1, "ron", 1, "s", 1), count("Harry's--Ron42"), "separators");
        equal(Map.of("one", 1, "two", 1), count("one\r\ntwo"), "line boundaries");
        equal(Map.of("caf", 1, "na", 1, "ve", 1), count("Café naïve"), "UTF-8 bytes are separators");
        equal(Map.of("at", 1, "az", 1, "z", 1), count("@at`az[z{"), "bytes next to letters");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            equal(Map.of("i", 1), count("I"), "locale-independent lowercase");
        } finally {
            Locale.setDefault(previous);
        }
        String longWord = "abcdefghijklmnopqrstuvwxyz".repeat(10);
        equal(Map.of(longWord, 2), count(longWord + " " + longWord.toUpperCase(Locale.ROOT)), "long word");
        String split = " ".repeat(8190) + "Harry Potter";
        equal(Map.of("harry", 1, "potter", 1), count(split), "word across read buffers");
        StringBuilder many = new StringBuilder();
        Map<String, Integer> manyExpected = new HashMap<>();
        for (int i = 0; i < 20_000; i++) {
            StringBuilder word = new StringBuilder();
            for (int n = i; ; n /= 26) {
                word.append((char) ('a' + n % 26));
                if (n < 26) {
                    break;
                }
            }
            many.append(word).append(' ').append(word).append(' ');
            manyExpected.put(word.toString(), 2);
        }
        equal(manyExpected, count(many.toString()), "table growth");

        var top = WordFrequencyAnalyzer.topWords(counts("b z a z b z a"), 30);
        equal(List.of("z", "a", "b"), top.stream().map(w -> w.word()).toList(), "ranking and ties");
        equal(List.of(3, 2, 2), top.stream().map(w -> w.count()).toList(), "ranked counts");
        equal(List.of("z", "a"), WordFrequencyAnalyzer.topWords(counts("b z a z b z a"), 2)
                .stream().map(w -> w.word()).toList(), "bounded ranking");
        equal(List.of("ab", "b"), WordFrequencyAnalyzer.topWords(counts("b ab"), 30)
                .stream().map(w -> w.word()).toList(), "prefix ties");
        equal(1, WordFrequencyAnalyzer.topWords(counts("a b b"), 1).size(), "limit");
        equal(List.of(), WordFrequencyAnalyzer.topWords(counts(""), 30), "empty ranking");
        for (int invalid : new int[]{0, -1}) {
            try {
                WordFrequencyAnalyzer.topWords(counts(""), invalid);
                throw new AssertionError("Invalid limit accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
        try {
            WordFrequencyAnalyzer.countWords(new InputStream() {
                public int read() throws IOException {
                    throw new IOException("read failed");
                }
            });
            throw new AssertionError("Read failure swallowed");
        } catch (IOException expected) {
            checks++;
        }

        // Independent whole-file split oracle for every word in the downloaded book.
        String book = Files.readString(Path.of("src", "txt", "harry.txt"), Charset.forName("windows-1252"));
        Map<String, Integer> expected = new java.util.TreeMap<>();
        for (String word : book.replaceAll("[^A-Za-z]", " ").toLowerCase(Locale.ROOT).split(" +")) {
            if (!word.isEmpty()) {
                expected.put(word, expected.getOrDefault(word, 0) + 1);
            }
        }
        try (InputStream file = Files.newInputStream(Path.of("src", "txt", "harry.txt"))) {
            equal(expected, toMap(WordFrequencyAnalyzer.countWords(file)), "full book against independent oracle");
        }
        Path sample = Files.createTempFile("word-frequency-test-", ".txt");
        try {
            Files.writeString(sample, "Ron Harry Ron", StandardCharsets.UTF_8);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
                 PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
                equal(0, Main.run(new String[]{sample.toString(), "30"}, out, err), "CLI success");
                equal(true, output.toString(StandardCharsets.UTF_8)
                        .matches("ron 2\\Rharry 1\\R------\\R[0-9]+\\R"), "CLI report format");
                equal("", errors.toString(StandardCharsets.UTF_8), "success stderr");
                for (String limit : new String[]{"0", "-1", "abc", "2147483648"}) {
                    equal(2, Main.run(new String[]{sample.toString(), limit}, out, err), "bad CLI limit");
                }
                equal(2, Main.run(new String[]{"a", "b", "c"}, out, err), "extra arguments");
                equal(1, Main.run(new String[]{sample.resolve("missing").toString()}, out, err), "missing input");
                equal(2, Main.run(new String[]{"\0"}, out, err), "invalid path");
                output.reset();
                Files.writeString(sample, "", StandardCharsets.UTF_8);
                equal(0, Main.run(new String[]{sample.toString()}, out, err), "empty CLI file");
                equal(true, output.toString(StandardCharsets.UTF_8).matches("------\\R[0-9]+\\R"), "empty report");
            }
        } finally {
            Files.deleteIfExists(sample);
        }
        System.out.println("Passed " + checks + " checks.");
    }

    private static WordCounts counts(String text) throws IOException {
        return WordFrequencyAnalyzer.countWords(
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Integer> count(String text) throws IOException {
        return toMap(counts(text));
    }

    private static Map<String, Integer> toMap(WordCounts counts) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < counts.size(); i++) {
            map.put(counts.word(i), counts.count(i));
        }
        return map;
    }

    private static void equal(Object expected, Object actual, String description) {
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected " + expected + ", actual " + actual);
        }
        checks++;
    }
}
