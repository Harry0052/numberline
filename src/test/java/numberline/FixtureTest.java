package numberline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import numberline.Model.ComparisonResult;
import numberline.Model.Finding;
import numberline.Model.LintReport;
import org.junit.jupiter.api.Test;

class FixtureTest {
    private static final Path ROOT = Path.of(".");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map<String, String> RULES = Map.ofEntries(
            Map.entry("incompatible_unit", "NL-UNIT-DIMENSION"),
            Map.entry("incompatible_currency", "NL-CURRENCY"),
            Map.entry("incompatible_period", "NL-PERIOD"),
            Map.entry("incompatible_population", "NL-POPULATION"),
            Map.entry("incompatible_numerator", "NL-NUMERATOR"),
            Map.entry("incompatible_denominator", "NL-DENOMINATOR"),
            Map.entry("unknown_missing_period", "NL-MISSING-PERIOD"),
            Map.entry("unknown_missing_population", "NL-MISSING-POPULATION"),
            Map.entry("unknown_missing_currency", "NL-MISSING-CURRENCY"),
            Map.entry("unknown_missing_numerator", "NL-MISSING-NUMERATOR"),
            Map.entry("unknown_missing_denominator", "NL-MISSING-DENOMINATOR"));

    @Test
    void manifestChecksumsMatchTheFrozenFiles() throws Exception {
        String manifest = Files.readString(ROOT.resolve("fixtures/MANIFEST.md"));
        Matcher matcher = Pattern.compile("`([^`]+)` sha256 `([0-9a-f]{64})`").matcher(manifest);
        int rows = 0;
        while (matcher.find()) {
            rows++;
            byte[] bytes = Files.readAllBytes(ROOT.resolve(matcher.group(1)));
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            assertEquals(matcher.group(2), digest, matcher.group(1));
        }
        assertEquals(9, rows);
    }

    @Test
    void validFixtureIsAcceptedAndInvalidFixturesAreRejected() throws Exception {
        byte[] document = Files.readAllBytes(ROOT.resolve("fixtures/research-report.md"));
        byte[] claims = Files.readAllBytes(ROOT.resolve("fixtures/claims.json"));
        LintReport report = NumberLine.lint(document, claims);
        assertEquals(24, report.summary().total());
        assertEquals("cd335abba1cfbd920e3485b7905b2624f0a5ff6105dd6e22c2f6d0683a510591", report.document().sha256());

        expectInvalid("fixtures/invalid/schema.json", "schema");
        expectInvalid("fixtures/invalid/bad-hash.json", "hash");
        expectInvalid("fixtures/invalid/span-split.json", "span");
        expectInvalid("fixtures/invalid/unknown-unit.json", "unit");
        expectInvalid("fixtures/invalid/nonfinite.json", "number");
    }

    @Test
    void frozenFixtureMatchesTheHandOracle() throws Exception {
        byte[] document = Files.readAllBytes(ROOT.resolve("fixtures/research-report.md"));
        String passage = new String(document, StandardCharsets.UTF_8);
        assertTrue(passage.contains("naïve"));
        assertTrue(passage.contains("患者"));
        assertTrue(passage.contains("µg"));
        JsonNode oracle = JSON.readTree(Files.readString(ROOT.resolve("fixtures/oracle.json")));
        LintReport report = NumberLine.lint(document, Files.readAllBytes(ROOT.resolve("fixtures/claims.json")));
        Map<String, Integer> verdicts = new LinkedHashMap<>();
        Map<String, Integer> issues = new LinkedHashMap<>();
        assertEquals(oracle.get("cases").size(), report.comparisons().size());
        for (int i = 0; i < report.comparisons().size(); i++) {
            JsonNode expected = oracle.get("cases").get(i);
            ComparisonResult actual = report.comparisons().get(i);
            assertEquals(expected.get("id").asText(), actual.id());
            assertEquals(expected.get("verdict").asText(), actual.verdict(), actual.id());
            assertEquals(texts(expected.get("issueClasses")), actual.issueClasses(), actual.id());
            assertEquals(texts(expected.get("missingFields")), actual.missingFields(), actual.id());
            assertEquals(texts(expected.get("basisFields")), actual.basisDifferences().stream().map(d -> d.field()).toList(), actual.id());
            assertEquals(expected.get("conversionAttempted").asBoolean(), actual.conversion().attempted(), actual.id());
            assertEquals(expected.get("conversionApplied").asBoolean(), actual.conversion().applied(), actual.id());
            assertEquals(textOrNull(expected.get("canonicalUnit")), actual.conversion().canonicalUnit(), actual.id());
            assertEquals(expected.get("normalizedStatus").asText(), actual.normalized().status(), actual.id());
            assertEquals(textOrNull(expected.get("leftCanonical")), actual.normalized().left(), actual.id());
            assertEquals(textOrNull(expected.get("rightCanonical")), actual.normalized().right(), actual.id());
            assertEquals(textOrNull(expected.get("canonicalUnit")), actual.normalized().unit(), actual.id());
            assertEquals(expected.get("arithmetic").asText(), actual.arithmetic().status(), actual.id());
            assertEquals(expected.get("leftExcerpt").asText(), actual.excerpts().left().text(), actual.id());
            assertEquals(expected.get("rightExcerpt").asText(), actual.excerpts().right().text(), actual.id());
            assertEquals(expectedRules(expected), actual.findings().stream().map(Finding::ruleId).toList(), actual.id());
            assertFalse(actual.sourceTruthChecked());
            int left = actual.excerpts().left().start();
            int right = actual.excerpts().right().end();
            assertTrue(Spans.isBoundary(document, left), actual.id());
            assertTrue(Spans.isBoundary(document, actual.excerpts().left().end()), actual.id());
            assertTrue(Spans.isBoundary(document, actual.excerpts().right().start()), actual.id());
            assertTrue(Spans.isBoundary(document, right), actual.id());
            verdicts.merge(actual.verdict(), 1, Integer::sum);
            for (String issue : actual.issueClasses()) {
                issues.merge(issue, 1, Integer::sum);
            }
        }
        assertEquals(10, verdicts.get("compatible"));
        assertEquals(9, verdicts.get("incompatible"));
        assertEquals(5, verdicts.get("unknown"));
        assertEquals(verdicts, Map.of(
                "compatible", report.summary().compatible(),
                "incompatible", report.summary().incompatible(),
                "unknown", report.summary().unknown()));
        assertEquals(issues, report.summary().byIssueClass());
        ComparisonResult first = report.comparisons().get(0);
        ComparisonResult reversed = report.comparisons().get(1);
        assertEquals(first.normalized().left(), reversed.normalized().right());
        assertEquals(first.normalized().right(), reversed.normalized().left());
    }

    @Test
    void proofCaseAndMissingField() throws Exception {
        byte[] document = Files.readAllBytes(ROOT.resolve("fixtures/research-report.md"));
        JsonNode claims = JSON.readTree(Files.readAllBytes(ROOT.resolve("fixtures/claims.json")));
        LintReport report = NumberLine.lint(document, JSON.writeValueAsBytes(claims));
        ComparisonResult mass = byId(report, "C01");
        assertEquals("compatible", mass.verdict());
        assertEquals("1200", mass.normalized().left());
        assertEquals("1200", mass.normalized().right());
        ComparisonResult rates = byId(report, "C06");
        assertEquals("incompatible", rates.verdict());
        assertEquals(List.of("incompatible_denominator"), rates.issueClasses());
        assertEquals("0.08", rates.normalized().left());
        assertEquals("0.08", rates.normalized().right());
        assertEquals("not_evaluated", rates.arithmetic().status());

        JsonNode removedDenominator = claims.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) removedDenominator.get("comparisons").get(5).get("left"))
                .putNull("denominator");
        LintReport unknown = NumberLine.lint(document, JSON.writeValueAsBytes(removedDenominator));
        ComparisonResult changed = byId(unknown, "C06");
        assertEquals("unknown", changed.verdict());
        assertEquals(List.of("unknown_missing_denominator"), changed.issueClasses());

        JsonNode removedPeriod = claims.deepCopy();
        com.fasterxml.jackson.databind.node.ObjectNode left =
                (com.fasterxml.jackson.databind.node.ObjectNode) removedPeriod.get("comparisons").get(5).get("left");
        left.putNull("periodStart");
        left.putNull("periodEnd");
        LintReport both = NumberLine.lint(document, JSON.writeValueAsBytes(removedPeriod));
        ComparisonResult mixed = byId(both, "C06");
        assertEquals("unknown", mixed.verdict());
        assertEquals(List.of("incompatible_denominator", "unknown_missing_period"), mixed.issueClasses());
        assertEquals(List.of("period"), mixed.missingFields());
        assertEquals(List.of("denominator"), mixed.basisDifferences().stream().map(d -> d.field()).toList());
    }

    @Test
    void reportsKeepMarkupInertAndQuoteTheSourceSpan() throws Exception {
        byte[] document = Files.readAllBytes(ROOT.resolve("fixtures/research-report.md"));
        byte[] claims = Files.readAllBytes(ROOT.resolve("fixtures/claims.json"));
        LintReport report = NumberLine.lint(document, claims);
        String text = Reports.text(report);
        String json = Reports.json(report);
        assertTrue(text.contains("summary: total=24 compatible=10 incompatible=9 unknown=5"));
        assertTrue(text.contains("C06 verdict=incompatible"));
        assertTrue(text.contains("NL-DENOMINATOR"));
        assertTrue(text.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(text.contains("**bold**"));
        assertFalse(text.contains("<script"));
        assertFalse(json.contains("<script"));
        assertFalse(json.contains("&lt;script&gt;"));
        assertTrue(json.contains("\\u003cscript\\u003e"));
        JsonNode parsed = JSON.readTree(json);
        String excerpt = null;
        for (JsonNode row : parsed.get("comparisons")) {
            if ("C11".equals(row.get("id").asText())) {
                excerpt = row.get("excerpts").get("right").get("text").asText();
            }
        }
        assertTrue(excerpt.contains("<script>alert(1)</script>"));
        assertTrue(excerpt.contains("**bold**"));
        int marker = text.indexOf("excerpt right source=C11R");
        assertTrue(marker > 0);
        int lineStart = text.lastIndexOf('\n', marker) + 1;
        String line = text.substring(lineStart, text.indexOf('\n', marker));
        assertTrue(line.startsWith("    excerpt right source=C11R"), line);
    }

    @Test
    void limitsRejectOversizedInputs() throws Exception {
        byte[] claims = Files.readAllBytes(ROOT.resolve("fixtures/claims.json"));
        InvalidInputException tooBig = assertThrows(
                InvalidInputException.class, () -> NumberLine.lint(new byte[Limits.MAX_DOCUMENT_BYTES + 1], claims));
        assertEquals("limit", tooBig.category());
    }

    private static void expectInvalid(String path, String category) throws Exception {
        byte[] document = Files.readAllBytes(ROOT.resolve("fixtures/research-report.md"));
        InvalidInputException ex = assertThrows(
                InvalidInputException.class, () -> NumberLine.lint(document, Files.readAllBytes(ROOT.resolve(path))));
        assertEquals(category, ex.category(), ex.diagnostic());
        assertTrue(ex.diagnostic().startsWith("invalid_input: " + category + ":"), ex.diagnostic());
    }

    private static ComparisonResult byId(LintReport report, String id) {
        return report.comparisons().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static List<String> expectedRules(JsonNode expected) {
        List<String> rules = new ArrayList<>();
        for (JsonNode issue : expected.get("issueClasses")) {
            rules.add(RULES.get(issue.asText()));
        }
        if ("compatible".equals(expected.get("verdict").asText())) {
            rules.add("NL-COMPATIBLE");
        }
        if (!expected.get("conversionAttempted").asBoolean()) {
            rules.add("NL-CONVERSION-NOT-ATTEMPTED");
        } else if (expected.get("conversionApplied").asBoolean()) {
            rules.add("NL-CONVERSION-APPLIED");
        } else {
            rules.add("NL-CONVERSION-IDENTITY");
        }
        rules.add(switch (expected.get("arithmetic").asText()) {
            case "equal" -> "NL-ARITHMETIC-EQUAL";
            case "mismatch" -> "NL-ARITHMETIC-MISMATCH";
            default -> "NL-ARITHMETIC-SKIPPED";
        });
        return rules;
    }
}
