/*
  @author   Sandoplay
  @project  Triangle
  @class    Triangle
  @version  1.0.0
  @since    08/10/2026
*/

import java.util.Arrays;

/**
 * An immutable, non-degenerate triangle with finite positive side lengths.
 * Side order is significant for equality, as in the original implementation.
 */
public final class Triangle {
    private final double a;
    private final double b;
    private final double c;

    /**
     * @throws IllegalArgumentException if any side is non-finite, non-positive,
     *         or the lengths do not satisfy the strict triangle inequality
     */
    public Triangle(double a, double b, double c) {
        validateSide(a, "a");
        validateSide(b, "b");
        validateSide(c, "c");

        double[] sides = sortedSides(a, b, c);
        // Subtraction avoids overflow in the usual sum of the two smaller sides.
        if (sides[0] <= sides[2] - sides[1]) {
            throw new IllegalArgumentException("The sides must satisfy the triangle inequality.");
        }

        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }

    /** Returns the perimeter; returns positive infinity if it exceeds double's range. */
    public double perimeter() {
        return a + b + c;
    }

    /**
     * Returns the area using a numerically stable rearrangement of Heron's formula.
     * A result outside double's range rounds to positive infinity or zero.
     */
    public double area() {
        double[] sides = sortedSides(a, b, c);
        double smallest = sides[0];
        double middle = sides[1];
        double largest = sides[2];
        double difference = largest - middle;

        // For x >= y >= z: 4 * area = sqrt((x+(y+z)) * (z-(x-y))
        //                                     * (z+(x-y)) * (x+(y-z))).
        // Compute square roots of the large factors without forming overflowing sums.
        double rootLargest = Math.sqrt(largest);
        double[] roots = {
            rootLargest * Math.sqrt(1.0 + middle / largest + smallest / largest),
            Math.sqrt(smallest - difference),
            Math.sqrt(smallest + difference),
            rootLargest * Math.sqrt(1.0 + (middle - smallest) / largest)
        };

        // Accumulate binary exponents separately so only the final result can
        // overflow or underflow, even when the sides have very different scales.
        double significand = 1.0;
        int exponent = -2; // Division by four in Heron's formula.
        for (double root : roots) {
            int rootExponent = Math.getExponent(root);
            significand *= Math.scalb(root, -rootExponent);
            exponent += rootExponent;
        }
        return Math.scalb(significand, exponent);
    }

    /** Uses exact equality of the supplied lengths, consistent with equals. */
    public boolean isEquilateral() {
        return a == b && b == c;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Triangle triangle)) {
            return false;
        }
        return Double.compare(a, triangle.a) == 0
            && Double.compare(b, triangle.b) == 0
            && Double.compare(c, triangle.c) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(a);
        result = 31 * result + Double.hashCode(b);
        result = 31 * result + Double.hashCode(c);
        return result;
    }

    @Override
    public String toString() {
        return "Triangle{a=" + a + ", b=" + b + ", c=" + c + '}';
    }

    private static void validateSide(double side, String name) {
        if (!Double.isFinite(side) || side <= 0.0) {
            throw new IllegalArgumentException("Side " + name + " must be finite and greater than zero.");
        }
    }

    private static double[] sortedSides(double a, double b, double c) {
        double[] sides = {a, b, c};
        Arrays.sort(sides);
        return sides;
    }
}
