package numberline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Input, validated, and report shapes. JSON field order follows these records. */
public final class Model {
    private Model() {}

    public record ReadyInput(
            String documentId, String sha256, byte[] document, List<ReadyComparison> comparisons) {}

    public record ReadyComparison(String id, ReadyOperand left, ReadyOperand right) {}

    public record ReadyOperand(
            String sourceId,
            int start,
            int end,
            String excerpt,
            BigDecimal value,
            Units.UnitDef unit,
            String currency,
            LocalDate periodStart,
            LocalDate periodEnd,
            String numerator,
            String denominator,
            String populationKey) {}

    public record LintReport(
            String schemaVersion,
            Tool tool,
            ReportDocument document,
            Summary summary,
            List<String> limitations,
            List<ComparisonResult> comparisons) {}

    public record Tool(String name, String version) {}

    public record ReportDocument(String id, String sha256, int byteLength) {}

    public record Summary(
            int total, int compatible, int incompatible, int unknown, Map<String, Integer> byIssueClass) {}

    public record ComparisonResult(
            String id,
            String verdict,
            List<String> issueClasses,
            List<String> missingFields,
            List<BasisDifference> basisDifferences,
            Conversion conversion,
            Normalized normalized,
            Arithmetic arithmetic,
            List<Finding> findings,
            Excerpts excerpts,
            String provenance,
            boolean sourceTruthChecked) {}

    public record BasisDifference(String field, String left, String right, String ruleId) {}

    public record Conversion(boolean attempted, boolean applied, String canonicalUnit) {}

    public record Normalized(String status, String left, String right, String unit) {}

    public record Arithmetic(String status, String reason) {}

    public record Finding(String ruleId, String field, String message) {}

    public record Excerpts(Excerpt left, Excerpt right) {}

    public record Excerpt(String sourceId, int start, int end, String text) {}
}
