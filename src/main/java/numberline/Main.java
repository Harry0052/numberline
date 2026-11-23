package numberline;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import numberline.Model.LintReport;

/**
 * Reviewer CLI.
 *
 * <p>Exit 0 when a report was written. Exit 1 for usage and file-path problems. Exit 2 when the
 * fixture is invalid. Exit 3 when {@code --fail-on findings} sees a result that is not compatible.
 * Diagnostics go to the error stream. The report goes to standard output or to {@code --output}.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || isHelp(args[0])) {
            out.print(help());
            return 0;
        }
        if ("version".equals(args[0]) || "--version".equals(args[0])) {
            out.println("numberline " + Version.VALUE);
            return 0;
        }
        if (!"lint".equals(args[0])) {
            err.println("Unknown command '" + args[0] + "'.");
            err.print(help());
            return 1;
        }
        Path document = null;
        Path claims = null;
        String format = "text";
        Path output = null;
        boolean failOnFindings = false;
        try {
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--document" -> document = Path.of(requireValue(args, ++i, arg));
                    case "--claims" -> claims = Path.of(requireValue(args, ++i, arg));
                    case "--format" -> format = requireValue(args, ++i, arg);
                    case "--output" -> output = Path.of(requireValue(args, ++i, arg));
                    case "--fail-on" -> {
                        String value = requireValue(args, ++i, arg);
                        if (!"none".equals(value) && !"findings".equals(value)) {
                            throw new UsageException("--fail-on must be none or findings");
                        }
                        failOnFindings = "findings".equals(value);
                    }
                    default -> throw new UsageException("Unknown option '" + arg + "'.");
                }
            }
        } catch (UsageException ex) {
            err.println(ex.getMessage());
            err.print(help());
            return 1;
        }
        if (document == null || claims == null) {
            err.println("lint requires --document and --claims.");
            err.print(help());
            return 1;
        }
        if (!"text".equals(format) && !"json".equals(format)) {
            err.println("--format must be text or json.");
            return 1;
        }
        if (!Files.isRegularFile(document)) {
            err.println("Document not found: " + document);
            return 1;
        }
        if (!Files.isRegularFile(claims)) {
            err.println("Claims not found: " + claims);
            return 1;
        }
        if (output != null && (sameFile(document, output) || sameFile(claims, output))) {
            err.println("--output must not replace the document or the claims file.");
            return 1;
        }
        byte[] documentBytes;
        byte[] claimBytes;
        try {
            documentBytes = readBounded(document, Limits.MAX_DOCUMENT_BYTES, "document");
            claimBytes = readBounded(claims, Limits.MAX_CLAIMS_BYTES, "claims");
        } catch (InvalidInputException ex) {
            err.println(ex.diagnostic());
            return 2;
        } catch (IOException ex) {
            err.println("Could not read input files.");
            return 1;
        }
        LintReport report;
        try {
            report = NumberLine.lint(documentBytes, claimBytes);
        } catch (InvalidInputException ex) {
            err.println(ex.diagnostic());
            return 2;
        }
        String rendered = "json".equals(format) ? Reports.json(report) : Reports.text(report);
        try {
            if (output == null) {
                out.print(rendered);
            } else {
                Path parent = output.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(output, rendered, StandardCharsets.UTF_8);
                err.println("wrote " + output);
            }
        } catch (IOException ex) {
            err.println("Could not write the report.");
            return 1;
        }
        if (failOnFindings && (report.summary().incompatible() > 0 || report.summary().unknown() > 0)) {
            return 3;
        }
        return 0;
    }

    private static byte[] readBounded(Path path, int limit, String label) throws IOException {
        long size = Files.size(path);
        if (size > limit) {
            throw new InvalidInputException("limit", label + " is " + size + " bytes; the limit is " + limit + " bytes");
        }
        return Files.readAllBytes(path);
    }

    private static boolean sameFile(Path left, Path right) {
        try {
            if (Files.exists(left) && Files.exists(right) && Files.isSameFile(left, right)) {
                return true;
            }
        } catch (IOException ex) {
            return false;
        }
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new UsageException(option + " requires a value.");
        }
        return args[index];
    }

    private static boolean isHelp(String arg) {
        return "help".equals(arg) || "--help".equals(arg) || "-h".equals(arg);
    }

    static String help() {
        return """
                NumberLine %s
                Flag numerical comparisons that use incompatible units, denominators, currencies, or periods.

                Usage:
                  numberline lint --document FILE --claims FILE [--format text|json] [--output FILE] [--fail-on none|findings]
                  numberline help
                  numberline version

                The report is the classification. Exit 0 means the report was written, including when some
                comparisons are incompatible or unknown. Exit 2 means the input is invalid. Exit 3 is reserved
                for --fail-on findings.

                Demo, after the package is built:
                  java -jar target/numberline-%s.jar lint \\
                    --document fixtures/research-report.md \\
                    --claims fixtures/claims.json \\
                    --format text \\
                    --output target/demo-report.txt
                """
                .formatted(Version.VALUE, Version.VALUE);
    }

    private static final class UsageException extends RuntimeException {
        UsageException(String message) {
            super(message);
        }
    }
}
