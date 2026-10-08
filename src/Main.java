/*
  @author   Sandoplay
  @project  Triangle
  @class    Main
  @version  1.0.0
  @since    08/10/2026
*/

import java.io.PrintStream;

/** Demonstrates triangle calculations with optional command-line side lengths. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream output, PrintStream errors) {
        if (args.length != 0 && args.length != 3) {
            errors.println("Usage: java -cp out Main [a b c]");
            return 1;
        }

        try {
            Triangle triangle = args.length == 0
                ? new Triangle(3.0, 4.0, 5.0)
                : new Triangle(Double.parseDouble(args[0]), Double.parseDouble(args[1]),
                    Double.parseDouble(args[2]));

            output.println(triangle);
            output.println("Perimeter: " + triangle.perimeter());
            output.println("Area: " + triangle.area());
            output.println("Equilateral: " + triangle.isEquilateral());
            return 0;
        } catch (IllegalArgumentException exception) {
            errors.println("Invalid triangle: " + exception.getMessage());
            return 1;
        }
    }
}
