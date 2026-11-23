package numberline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {
    @TempDir
    Path temp;

    @Test
    void helpAndVersionDoNotLookLikeFailures() {
        Captured help = run("help");
        assertEquals(0, help.code);
        assertTrue(help.out.contains("numberline lint"));
        assertEquals("", help.err);
        Captured version = run("version");
        assertEquals(0, version.code);
        assertTrue(version.out.contains("numberline " + Version.VALUE));
    }

    @Test
    void demoPathWritesAReportAndABadFileExplainsItself() throws Exception {
        Path text = temp.resolve("demo-report.txt");
        Path json = temp.resolve("demo-report.json");
        Captured textRun = run(
                "lint",
                "--document",
                "fixtures/research-report.md",
                "--claims",
                "fixtures/claims.json",
                "--format",
                "text",
                "--output",
                text.toString());
        assertEquals(0, textRun.code, textRun.err);
        assertEquals("", textRun.out);
        assertTrue(textRun.err.contains("wrote"));
        String report = Files.readString(text);
        assertTrue(report.contains("summary: total=24 compatible=10 incompatible=9 unknown=5"));
        assertTrue(report.contains("excerpt left source=C01L"));
        assertTrue(report.contains("orders placed"));

        Captured jsonRun = run(
                "lint",
                "--document",
                "fixtures/research-report.md",
                "--claims",
                "fixtures/claims.json",
                "--format",
                "json",
                "--output",
                json.toString());
        assertEquals(0, jsonRun.code, jsonRun.err);
        assertTrue(Files.readString(json).contains("\"verdict\" : \"incompatible\""));

        Path rejected = temp.resolve("should-not-exist.txt");
        Captured invalid = run(
                "lint",
                "--document",
                "fixtures/research-report.md",
                "--claims",
                "fixtures/invalid/nonfinite.json",
                "--format",
                "text",
                "--output",
                rejected.toString());
        assertEquals(2, invalid.code);
        assertEquals("", invalid.out);
        assertTrue(invalid.err.contains("invalid_input: number:"));
        assertTrue(invalid.err.contains("NaN"));
        assertFalse(Files.exists(rejected));

        Captured usage = run("lint", "--document", "fixtures/research-report.md");
        assertEquals(1, usage.code);
        assertTrue(usage.err.contains("requires --document and --claims"));

        Captured findings = run(
                "lint",
                "--document",
                "fixtures/research-report.md",
                "--claims",
                "fixtures/claims.json",
                "--fail-on",
                "findings",
                "--output",
                temp.resolve("findings.txt").toString());
        assertEquals(3, findings.code);
        assertTrue(Files.exists(temp.resolve("findings.txt")));
    }

    private static Captured run(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        int code;
        try (PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8)) {
            code = Main.run(args, out, err);
        }
        return new Captured(
                code,
                outBytes.toString(StandardCharsets.UTF_8),
                errBytes.toString(StandardCharsets.UTF_8));
    }

    private record Captured(int code, String out, String err) {}
}
