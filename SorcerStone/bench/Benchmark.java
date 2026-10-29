import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Compares v1 (original), v2 (previous refactoring) and v3 (current src/).
 *
 * <pre>
 * java -cp out/bench Benchmark          full comparison (run from SorcerStone/)
 * java -cp out/bench Benchmark once v3      one cold run, used by the child JVMs
 * java -cp out/bench Benchmark retained v3  heap held by the word counts, also a child JVM
 * </pre>
 */
public final class Benchmark {
    private static final Path BOOK = Path.of("src", "txt", "harry.txt");
    private static final List<String> VERSIONS = List.of("v1", "v2", "v3");
    private static final int COLD_RUNS = 5;
    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private Benchmark() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("once")) {
            Measurement run = measure(args[1]);
            System.out.println(run.nanos + " " + run.allocatedBytes);
            return;
        }
        if (args.length == 2 && args[0].equals("retained")) {
            System.out.println(retainedBytes(args[1]));
            return;
        }
        checkSameReport("v2", "v3");
        System.out.printf(Locale.ROOT, "Java %s, %s, %d CPUs%n%n", System.getProperty("java.version"),
                System.getProperty("os.name"), Runtime.getRuntime().availableProcessors());
        System.out.println("version | cold ms (median of " + COLD_RUNS + " JVMs) | warm ms median"
                + " | warm ms min | allocated MB per run | counts retained KB | minimal -Xmx");
        for (String version : VERSIONS) {
            double cold = median(coldRuns(version));
            boolean slow = version.equals("v1");
            List<Measurement> warm = warmRuns(version, slow ? 3 : 50, slow ? 10 : 200);
            double[] millis = warm.stream().mapToDouble(m -> m.nanos / 1e6).toArray();
            double allocated = median(warm.stream().mapToDouble(m -> m.allocatedBytes / 1e6).toArray());
            String retained = version.equals("v1") ? "-"
                    : String.format(Locale.ROOT, "%.0f", Long.parseLong(child("retained", version, null).output.trim()) / 1024.0);
            System.out.printf(Locale.ROOT, "%s | %.1f | %.2f | %.2f | %.2f | %s | %s%n", version, cold,
                    median(millis), Arrays.stream(millis).min().orElseThrow(), allocated, retained,
                    minimalHeap(version));
        }
    }

    private static void run(String version, PrintStream out) throws IOException {
        int status = switch (version) {
            case "v1" -> {
                v1.OriginalMain.run(BOOK, out);
                yield 0;
            }
            case "v2" -> v2.Main.run(new String[]{BOOK.toString()}, out, System.err);
            case "v3" -> Main.run(new String[]{BOOK.toString()}, out, System.err);
            default -> throw new IllegalArgumentException("Unknown version " + version);
        };
        if (status != 0) {
            throw new IllegalStateException(version + " exited with " + status);
        }
    }

    private static Measurement measure(String version) throws IOException {
        PrintStream discard = new PrintStream(OutputStream.nullOutputStream());
        long allocatedBefore = THREADS.getCurrentThreadAllocatedBytes();
        long start = System.nanoTime();
        run(version, discard);
        long nanos = System.nanoTime() - start;
        return new Measurement(nanos, THREADS.getCurrentThreadAllocatedBytes() - allocatedBefore);
    }

    private static List<Measurement> warmRuns(String version, int warmup, int measured)
            throws IOException {
        for (int i = 0; i < warmup; i++) {
            measure(version);
        }
        List<Measurement> runs = new ArrayList<>();
        for (int i = 0; i < measured; i++) {
            System.gc();
            runs.add(measure(version));
        }
        return runs;
    }

    private static double[] coldRuns(String version) throws Exception {
        double[] millis = new double[COLD_RUNS];
        for (int i = 0; i < COLD_RUNS; i++) {
            String output = child("once", version, null).output;
            millis[i] = Long.parseLong(output.trim().split(" ")[0]) / 1e6;
        }
        return millis;
    }

    /** Smallest -Xmx (KiB precision) with which one cold run of the version succeeds. */
    private static String minimalHeap(String version) throws Exception {
        int low = 1024;
        int high = 256 * 1024;
        if (child("once", version, high).exitCode != 0) {
            return "> " + high / 1024 + " MB";
        }
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (child("once", version, middle).exitCode == 0) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return String.format(Locale.ROOT, "%.1f MB", high / 1024.0);
    }

    /** Heap still in use by the finished frequency table, measured after full GCs. */
    private static long retainedBytes(String version) throws IOException {
        long before = usedHeapAfterGc();
        Object counts = switch (version) {
            case "v2" -> {
                try (var reader = Files.newBufferedReader(BOOK, Charset.forName("windows-1252"))) {
                    yield v2.WordFrequencyAnalyzer.countWords(reader);
                }
            }
            case "v3" -> {
                try (var input = Files.newInputStream(BOOK)) {
                    yield WordFrequencyAnalyzer.countWords(input);
                }
            }
            default -> throw new IllegalArgumentException("No frequency table in " + version);
        };
        long after = usedHeapAfterGc();
        Reference.reachabilityFence(counts);
        return after - before;
    }

    private static long usedHeapAfterGc() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < 5; i++) {
            System.gc();
        }
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static ChildResult child(String mode, String version, Integer heapKib) throws Exception {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-XX:+UseSerialGC"));
        if (heapKib != null) {
            command.add("-Xmx" + heapKib + "k");
        }
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), "Benchmark", mode, version));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (heapKib == null && exitCode != 0) {
            throw new IllegalStateException(version + " failed: " + output);
        }
        return new ChildResult(exitCode, output);
    }

    private static void checkSameReport(String first, String second) throws IOException {
        if (!report(first).equals(report(second))) {
            throw new IllegalStateException(first + " and " + second + " print different reports");
        }
    }

    /** Report without the trailing elapsed-time line. */
    private static String report(String version) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        run(version, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String text = bytes.toString(StandardCharsets.UTF_8).stripTrailing();
        return text.substring(0, text.lastIndexOf('\n'));
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    private record Measurement(long nanos, long allocatedBytes) {
    }

    private record ChildResult(int exitCode, String output) {
    }
}
