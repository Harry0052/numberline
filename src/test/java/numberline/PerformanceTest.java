package numberline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PerformanceTest {
    @Test
    void fixtureClassifiesInsideTheLocalBudget() throws Exception {
        byte[] document = Files.readAllBytes(Path.of("fixtures/research-report.md"));
        byte[] claims = Files.readAllBytes(Path.of("fixtures/claims.json"));
        NumberLine.lint(document, claims);
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            NumberLine.lint(document, claims);
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
        assertTrue(elapsedMillis < 2_000, "50 in-process lints took " + elapsedMillis + " ms");
    }
}
