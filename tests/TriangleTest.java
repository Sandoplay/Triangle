/*
  @author   Sandoplay
  @project  Triangle
  @class    TriangleTest
  @version  1.0.0
  @since    08/10/2026
*/

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Regression tests without external dependencies; failures exit with a non-zero code. */
public final class TriangleTest {
    private static int checks;

    private TriangleTest() {
    }

    public static void main(String[] args) {
        testCalculations();
        testInvalidSides();
        testTriangleInequality();
        testEquality();
        testNumericalLimits();
        testCommandLine();
        System.out.println("Passed " + checks + " checks.");
    }

    private static void testCalculations() {
        Triangle triangle = new Triangle(3.0, 4.0, 5.0);
        close(3.0, triangle.getA(), "side a");
        close(4.0, triangle.getB(), "side b");
        close(5.0, triangle.getC(), "side c");
        close(12.0, triangle.perimeter(), "3-4-5 perimeter");
        close(6.0, triangle.area(), "3-4-5 area");
        check(!triangle.isEquilateral(), "scalene triangle");
        check(triangle.toString().equals("Triangle{a=3.0, b=4.0, c=5.0}"), "string representation");

        Triangle equilateral = new Triangle(2.0, 2.0, 2.0);
        check(equilateral.isEquilateral(), "equilateral triangle");
        close(Math.sqrt(3.0), equilateral.area(), "equilateral area");
        check(!new Triangle(2.0, 2.0, 3.0).isEquilateral(), "isosceles triangle");
        close(0.75, new Triangle(0.25, 0.25, 0.25).perimeter(), "fractional perimeter");
        close(Math.sqrt(3.0) / 64.0, new Triangle(0.25, 0.25, 0.25).area(), "fractional area");

        for (double[] sides : permutations(3.0, 4.0, 5.0)) {
            close(6.0, new Triangle(sides[0], sides[1], sides[2]).area(), "area for permuted sides");
        }

        double base = Math.nextDown(2.0);
        double expected = 0.25 * base * Math.sqrt((2.0 - base) * (2.0 + base));
        close(expected, new Triangle(1.0, 1.0, base).area(), "nearly degenerate area");
        check(!new Triangle(1.0, 1.0, Math.nextUp(1.0)).isEquilateral(), "exact side comparison");
    }

    private static void testInvalidSides() {
        double[] invalidSides = {
            0.0, -0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double side : invalidSides) {
            rejects(() -> new Triangle(side, 2.0, 2.0), "invalid side a: " + side);
            rejects(() -> new Triangle(2.0, side, 2.0), "invalid side b: " + side);
            rejects(() -> new Triangle(2.0, 2.0, side), "invalid side c: " + side);
        }
        rejects(() -> new Triangle(0.0, 0.0, 0.0), "all sides zero");
    }

    private static void testTriangleInequality() {
        for (double[] sides : permutations(1.0, 2.0, 3.0)) {
            rejects(() -> new Triangle(sides[0], sides[1], sides[2]), "degenerate triangle");
        }
        for (double[] sides : permutations(1.0, 2.0, 4.0)) {
            rejects(() -> new Triangle(sides[0], sides[1], sides[2]), "triangle inequality");
        }
        rejects(() -> new Triangle(1.0, 1.0, Math.nextUp(2.0)), "just beyond degeneracy");
    }

    private static void testEquality() {
        Triangle first = new Triangle(3.0, 4.0, 5.0);
        Triangle second = new Triangle(3.0, 4.0, 5.0);
        Triangle third = new Triangle(3.0, 4.0, 5.0);

        check(first.equals(first), "reflexive equality");
        check(first.equals(second) && second.equals(first), "symmetric equality");
        check(first.equals(second) && second.equals(third) && first.equals(third), "transitive equality");
        check(!first.equals(null), "null comparison");
        check(!first.equals("triangle"), "different type");
        check(!first.equals(new Triangle(4.0, 3.0, 5.0)), "side order remains significant");
        check(!first.equals(new Triangle(3.0, 4.0, 6.0)), "different triangle");
        check(first.hashCode() == second.hashCode(), "equal hash codes");

        Set<Triangle> triangles = new HashSet<>();
        triangles.add(first);
        check(triangles.contains(second), "value lookup in HashSet");
        int originalHash = first.hashCode();
        first.area();
        first.perimeter();
        first.isEquilateral();
        check(first.hashCode() == originalHash && triangles.contains(first), "stable collection key");
    }

    private static void testNumericalLimits() {
        close(Math.sqrt(3.0) / 4.0 * 1.0e300,
            new Triangle(1.0e150, 1.0e150, 1.0e150).area(), "large finite area");
        close(Math.sqrt(3.0) / 4.0 * 1.0e-300,
            new Triangle(1.0e-150, 1.0e-150, 1.0e-150).area(), "small finite area");
        close(Math.sqrt(3.0) / 4.0 * 1.0e308,
            new Triangle(1.0e154, 1.0e154, 1.0e154).area(), "finite area near upper range");

        for (double[] sides : permutations(1.0, 1.0, 1.0e-200)) {
            close(5.0e-201, new Triangle(sides[0], sides[1], sides[2]).area(), "very thin triangle");
        }
        close(0.5, new Triangle(1.0e200, 1.0e200, 1.0e-200).area(), "mixed scales");
        close((Double.MAX_VALUE / 2.0) * Double.MIN_VALUE,
            new Triangle(Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE).area(),
            "extreme finite sides");

        Triangle maximum = new Triangle(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        check(maximum.perimeter() == Double.POSITIVE_INFINITY, "true perimeter overflow");
        check(maximum.area() == Double.POSITIVE_INFINITY, "true area overflow");
        check(new Triangle(1.0e-200, 1.0e-200, 1.0e-200).area() == 0.0, "true area underflow");
        check(new Triangle(1.0e-161, 1.0e-161, 1.0e-161).area() > 0.0, "subnormal area retained");
        check(new Triangle(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE).isEquilateral(),
            "minimum positive sides accepted");
    }

    private static void testCommandLine() {
        CliResult defaults = invoke();
        check(defaults.exitCode == 0, "default CLI exit");
        check(defaults.output.contains("Perimeter: 12.0"), "default CLI example");
        check(defaults.output.contains("Area: 6.0"), "default CLI area");
        check(defaults.errors.isEmpty(), "default CLI stderr");

        CliResult equilateral = invoke("2", "2", "2");
        check(equilateral.exitCode == 0 && equilateral.output.contains("Equilateral: true"),
            "custom CLI sides");

        for (String[] arguments : new String[][] {
            {"1"}, {"1", "2"}, {"1", "2", "3", "4"}
        }) {
            CliResult result = invoke(arguments);
            check(result.exitCode == 1 && result.errors.contains("Usage:") && result.output.isEmpty(),
                "CLI argument count");
        }

        for (String[] arguments : new String[][] {
            {"abc", "2", "2"}, {"0", "2", "2"}, {"1", "2", "3"},
            {"NaN", "2", "2"}, {"Infinity", "2", "2"}
        }) {
            CliResult result = invoke(arguments);
            check(result.exitCode == 1 && result.errors.contains("Invalid triangle:")
                && result.output.isEmpty() && !result.errors.contains("at Main."),
                "CLI validation error");
        }
    }

    private static CliResult invoke(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            int exitCode = Main.run(args, out, err);
            return new CliResult(exitCode, output.toString(StandardCharsets.UTF_8),
                errors.toString(StandardCharsets.UTF_8));
        }
    }

    private record CliResult(int exitCode, String output, String errors) {
    }

    private static double[][] permutations(double a, double b, double c) {
        return new double[][] {
            {a, b, c}, {a, c, b}, {b, a, c}, {b, c, a}, {c, a, b}, {c, b, a}
        };
    }

    private static void rejects(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage() != null && !expected.getMessage().isBlank(), description);
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException: " + description);
    }

    private static void close(double expected, double actual, String description) {
        check(Double.isFinite(actual) && Math.abs(actual / expected - 1.0) <= 1.0e-12,
            description + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
