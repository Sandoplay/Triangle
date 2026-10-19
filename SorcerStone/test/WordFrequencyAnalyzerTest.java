import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dependency-free regression tests. Failures throw AssertionError without -ea. */
public final class WordFrequencyAnalyzerTest {
    private static int checks;

    public static void main(String[] args) throws IOException {
        equal(Map.of("harry", 3L, "ron", 1L), count("Harry HARRY harry Ron"), "case");
        equal(Map.of("hermione", 1L, "he", 2L), count("hermione he he"), "substrings");
        equal(Map.of(), count(""), "empty file");
        equal(Map.of(), count(" \n\t123!?Ж"), "no English words");
        equal(Map.of("harry", 1L, "ron", 1L, "s", 1L), count("Harry's--Ron42"), "separators");
        equal(Map.of("one", 1L, "two", 1L), count("one\r\ntwo"), "line boundaries");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            equal(Map.of("i", 1L), count("I"), "locale-independent lowercase");
        } finally {
            Locale.setDefault(previous);
        }
        var top = WordFrequencyAnalyzer.topWords(Map.of("z", 3L, "b", 2L, "a", 2L), 30);
        equal(List.of("z", "a", "b"), top.stream().map(w -> w.word()).toList(), "ranking and ties");
        equal(1, WordFrequencyAnalyzer.topWords(Map.of("a", 1L, "b", 2L), 1).size(), "limit");
        equal(List.of(), WordFrequencyAnalyzer.topWords(Map.of(), 30), "empty ranking");
        equal(3_000_000_000L, WordFrequencyAnalyzer.topWords(Map.of("a", 3_000_000_000L), 1)
                .get(0).count(), "long count");
        for (int invalid : new int[]{0, -1}) {
            try {
                WordFrequencyAnalyzer.topWords(Map.of(), invalid);
                throw new AssertionError("Invalid limit accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
        try {
            WordFrequencyAnalyzer.countWords(new Reader() {
                public int read(char[] buffer, int offset, int length) throws IOException {
                    throw new IOException("read failed");
                }
                public void close() { }
            });
            throw new AssertionError("Read failure swallowed");
        } catch (IOException expected) {
            checks++;
        }

        // Independent whole-file split oracle for every word in the downloaded book.
        String book = Files.readString(Path.of("src", "txt", "harry.txt"), Charset.forName("windows-1252"));
        Map<String, Long> expected = new java.util.TreeMap<>();
        for (String word : book.replaceAll("[^A-Za-z]", " ").toLowerCase(Locale.ROOT).split(" +")) {
            if (!word.isEmpty()) {
                expected.put(word, expected.getOrDefault(word, 0L) + 1);
            }
        }
        equal(expected, count(book), "full book against independent oracle");
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

    private static Map<String, Long> count(String text) throws IOException {
        return WordFrequencyAnalyzer.countWords(new StringReader(text));
    }

    private static void equal(Object expected, Object actual, String description) {
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected " + expected + ", actual " + actual);
        }
        checks++;
    }
}
