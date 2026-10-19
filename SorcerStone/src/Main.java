import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/** Console entry point for the word frequency report. */
public final class Main {
    private static final Path DEFAULT_INPUT = Path.of("src", "txt", "harry.txt");
    private static final int DEFAULT_LIMIT = 30;
    // The upstream book contains Windows-1252 punctuation (0x93 and 0x96).
    private static final Charset INPUT_CHARSET = Charset.forName("windows-1252");

    private Main() {
    }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintStream output, PrintStream errors) {
        if (args.length > 2) {
            errors.println("Usage: java Main [input-file] [positive-limit]");
            return 2;
        }
        final Path input;
        final int limit;
        try {
            input = args.length > 0 ? Path.of(args[0]) : DEFAULT_INPUT;
            limit = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_LIMIT;
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be positive.");
            }
        } catch (IllegalArgumentException exception) {
            errors.println("Invalid arguments: " + exception.getMessage());
            return 2;
        }
        long start = System.nanoTime();
        try (var reader = Files.newBufferedReader(input, INPUT_CHARSET)) {
            var frequencies = WordFrequencyAnalyzer.countWords(reader);
            for (var word : WordFrequencyAnalyzer.topWords(frequencies, limit)) {
                output.println(word.word() + " " + word.count());
            }
        } catch (IOException exception) {
            errors.println("Cannot read '" + input + "': " + exception.getMessage());
            return 1;
        }
        output.println("------");
        output.println((System.nanoTime() - start) / 1_000_000);
        return 0;
    }
}
