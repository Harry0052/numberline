package numberline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import numberline.Model.Arithmetic;
import numberline.Model.BasisDifference;
import numberline.Model.ComparisonResult;
import numberline.Model.Conversion;
import numberline.Model.Excerpt;
import numberline.Model.Excerpts;
import numberline.Model.Finding;
import numberline.Model.LintReport;
import numberline.Model.Normalized;
import numberline.Model.ReadyComparison;
import numberline.Model.ReadyInput;
import numberline.Model.ReadyOperand;
import numberline.Model.ReportDocument;
import numberline.Model.Summary;
import numberline.Model.Tool;
import numberline.Units.Dimension;
import numberline.Units.UnitDef;

/** Basis classification first. Magnitudes are compared only when the basis is compatible. */
public final class Engine {
    static final String PROVENANCE = "declared inputs only; source truth is not checked";
    static final List<String> LIMITATIONS = List.of(
            "Classifications use the declared claim fields. They do not establish that a source document is true.",
            "Physical units convert only through the built-in allowlist. Unknown units are rejected before normalization.",
            "Rates keep numerator, denominator, and population identity. A shared percent scale does not make those bases the same.",
            "Currency codes are identity checks. Exchange rates and inflation across years are not applied.",
            "A missing required basis field is unknown. Malformed input is a separate diagnostic, not a classification.",
            "Text reports escape HTML, and JSON escapes markup, so quoted Markdown and annotations stay literal.");

    private Engine() {}

    public static LintReport report(ReadyInput input) {
        List<ComparisonResult> results = new ArrayList<>();
        EnumMap<IssueClass, Integer> counts = new EnumMap<>(IssueClass.class);
        int compatible = 0;
        int incompatible = 0;
        int unknown = 0;
        for (ReadyComparison comparison : input.comparisons()) {
            ComparisonResult result = classify(comparison);
            results.add(result);
            switch (result.verdict()) {
                case "compatible" -> compatible++;
                case "incompatible" -> incompatible++;
                case "unknown" -> unknown++;
                default -> throw new IllegalStateException(result.verdict());
            }
            for (String issue : result.issueClasses()) {
                IssueClass parsed = wire(issue);
                counts.merge(parsed, 1, Integer::sum);
            }
        }
        Map<String, Integer> byIssue = new LinkedHashMap<>();
        for (IssueClass issue : IssueClass.values()) {
            int count = counts.getOrDefault(issue, 0);
            if (count > 0) {
                byIssue.put(issue.wire(), count);
            }
        }
        return new LintReport(
                "1",
                new Tool("numberline", Version.VALUE),
                new ReportDocument(input.documentId(), input.sha256(), input.document().length),
                new Summary(results.size(), compatible, incompatible, unknown, byIssue),
                LIMITATIONS,
                List.copyOf(results));
    }

    public static ComparisonResult classify(ReadyComparison comparison) {
        ReadyOperand left = comparison.left();
        ReadyOperand right = comparison.right();
        List<IssueClass> issues = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<BasisDifference> differences = new ArrayList<>();

        boolean sameDimension = left.unit().dimension() == right.unit().dimension();
        if (!sameDimension) {
            issues.add(IssueClass.INCOMPATIBLE_UNIT);
            differences.add(difference(IssueClass.INCOMPATIBLE_UNIT, left.unit().id(), right.unit().id()));
        }

        if (left.periodStart() == null || right.periodStart() == null) {
            missing.add("period");
            issues.add(IssueClass.UNKNOWN_MISSING_PERIOD);
        } else if (!sameRange(left, right)) {
            issues.add(IssueClass.INCOMPATIBLE_PERIOD);
            differences.add(difference(IssueClass.INCOMPATIBLE_PERIOD, range(left), range(right)));
        }

        if (left.populationKey() == null || right.populationKey() == null) {
            missing.add("population");
            issues.add(IssueClass.UNKNOWN_MISSING_POPULATION);
        } else if (!left.populationKey().equals(right.populationKey())) {
            issues.add(IssueClass.INCOMPATIBLE_POPULATION);
            differences.add(difference(
                    IssueClass.INCOMPATIBLE_POPULATION, left.populationKey(), right.populationKey()));
        }

        if (sameDimension && left.unit().dimension() == Dimension.MONEY) {
            if (left.currency() == null || right.currency() == null) {
                missing.add("currency");
                issues.add(IssueClass.UNKNOWN_MISSING_CURRENCY);
            } else if (!left.currency().equals(right.currency())) {
                issues.add(IssueClass.INCOMPATIBLE_CURRENCY);
                differences.add(difference(IssueClass.INCOMPATIBLE_CURRENCY, left.currency(), right.currency()));
            }
        }

        if (sameDimension && left.unit().dimension() == Dimension.RATE) {
            compareText(
                    issues,
                    differences,
                    missing,
                    "numerator",
                    IssueClass.UNKNOWN_MISSING_NUMERATOR,
                    IssueClass.INCOMPATIBLE_NUMERATOR,
                    left.numerator(),
                    right.numerator());
            compareText(
                    issues,
                    differences,
                    missing,
                    "denominator",
                    IssueClass.UNKNOWN_MISSING_DENOMINATOR,
                    IssueClass.INCOMPATIBLE_DENOMINATOR,
                    left.denominator(),
                    right.denominator());
        }

        issues.sort(Enum::compareTo);
        differences.sort((a, b) -> ruleOrder(a.ruleId()) - ruleOrder(b.ruleId()));

        boolean unknown = !missing.isEmpty();
        String verdict = unknown ? "unknown" : differences.isEmpty() && sameDimension ? "compatible" : "incompatible";
        // sameDimension is already represented by a difference when false. The extra clause keeps
        // a dimension clash incompatible even if that difference were omitted.
        if (!unknown && !sameDimension) {
            verdict = "incompatible";
        }

        Scale scale = scale(left, right, sameDimension);
        Arithmetic arithmetic = arithmetic(verdict, scale);
        List<Finding> findings = findings(issues, verdict, scale, arithmetic);

        return new ComparisonResult(
                comparison.id(),
                verdict,
                issues.stream().map(IssueClass::wire).toList(),
                List.copyOf(missing),
                List.copyOf(differences),
                new Conversion(scale.attempted(), scale.applied(), scale.unit()),
                new Normalized(scale.shown() ? "shown" : "not_evaluated", scale.left(), scale.right(), scale.unit()),
                arithmetic,
                findings,
                new Excerpts(excerpt(left), excerpt(right)),
                PROVENANCE,
                false);
    }

    private static void compareText(
            List<IssueClass> issues,
            List<BasisDifference> differences,
            List<String> missing,
            String field,
            IssueClass missingIssue,
            IssueClass differIssue,
            String left,
            String right) {
        if (left == null || right == null) {
            missing.add(field);
            issues.add(missingIssue);
        } else if (!left.equals(right)) {
            issues.add(differIssue);
            differences.add(difference(differIssue, left, right));
        }
    }

    private static Scale scale(ReadyOperand left, ReadyOperand right, boolean sameDimension) {
        if (!sameDimension) {
            return Scale.skipped();
        }
        if (left.unit().dimension() == Dimension.MONEY) {
            if (left.currency() == null || right.currency() == null) {
                if (left.currency() == null && right.currency() == null) {
                    return Scale.of(left, right, "money");
                }
                return Scale.skipped();
            }
            if (!left.currency().equals(right.currency())) {
                return Scale.skipped();
            }
            return Scale.of(left, right, left.currency());
        }
        String unit = left.unit().canonicalName(null);
        return Scale.of(left, right, unit);
    }

    private static Arithmetic arithmetic(String verdict, Scale scale) {
        if (!"compatible".equals(verdict)) {
            String reason = "unknown".equals(verdict) ? "basis_unknown" : "basis_incompatible";
            return new Arithmetic("not_evaluated", reason);
        }
        boolean equal = new BigDecimal(scale.left()).compareTo(new BigDecimal(scale.right())) == 0;
        return new Arithmetic(equal ? "equal" : "mismatch", null);
    }

    private static List<Finding> findings(List<IssueClass> issues, String verdict, Scale scale, Arithmetic arithmetic) {
        List<Finding> findings = new ArrayList<>();
        for (IssueClass issue : issues) {
            findings.add(new Finding(issue.ruleId(), issue.field(), message(issue)));
        }
        if ("compatible".equals(verdict)) {
            findings.add(new Finding(
                    "NL-COMPATIBLE",
                    null,
                    "The declared basis matches, so the normalized magnitudes can be compared."));
        }
        if (!scale.attempted()) {
            findings.add(new Finding("NL-CONVERSION-NOT-ATTEMPTED", "unit", "Conversion was not attempted."));
        } else if (scale.applied()) {
            findings.add(new Finding(
                    "NL-CONVERSION-APPLIED",
                    "unit",
                    "Converted both sides to " + scale.unit() + " using the allowlist."));
        } else {
            findings.add(new Finding(
                    "NL-CONVERSION-IDENTITY",
                    "unit",
                    "Both sides already use canonical unit " + scale.unit() + ". No scale factor was applied."));
        }
        findings.add(switch (arithmetic.status()) {
            case "equal" -> new Finding("NL-ARITHMETIC-EQUAL", "value", "Normalized magnitudes are equal.");
            case "mismatch" -> new Finding(
                    "NL-ARITHMETIC-MISMATCH",
                    "value",
                    "Normalized magnitudes differ. The basis is still compatible.");
            default -> new Finding(
                    "NL-ARITHMETIC-SKIPPED",
                    "value",
                    "Magnitudes were not compared because the basis is " + verdict + ".");
        });
        return List.copyOf(findings);
    }

    private static String message(IssueClass issue) {
        return switch (issue) {
            case INCOMPATIBLE_UNIT -> "Units are different dimensions, so no conversion was attempted.";
            case INCOMPATIBLE_CURRENCY -> "Currencies differ. NumberLine does not apply an exchange rate.";
            case INCOMPATIBLE_PERIOD ->
                    "Reporting periods differ. NumberLine does not prorate or inflate across windows.";
            case INCOMPATIBLE_POPULATION -> "Population keys differ.";
            case INCOMPATIBLE_NUMERATOR -> "Numerator definitions differ.";
            case INCOMPATIBLE_DENOMINATOR -> "Denominator definitions differ.";
            case UNKNOWN_MISSING_PERIOD ->
                    "Period start and end are required on both sides before this comparison can be classified.";
            case UNKNOWN_MISSING_POPULATION ->
                    "A population key is required on both sides before this comparison can be classified.";
            case UNKNOWN_MISSING_CURRENCY ->
                    "A currency code is required on both money amounts before this comparison can be classified.";
            case UNKNOWN_MISSING_NUMERATOR ->
                    "A numerator definition is required on both rates before this comparison can be classified.";
            case UNKNOWN_MISSING_DENOMINATOR ->
                    "A denominator definition is required on both rates before this comparison can be classified.";
        };
    }

    private static BasisDifference difference(IssueClass issue, String left, String right) {
        return new BasisDifference(issue.field(), left, right, issue.ruleId());
    }

    private static boolean sameRange(ReadyOperand left, ReadyOperand right) {
        return left.periodStart().equals(right.periodStart()) && left.periodEnd().equals(right.periodEnd());
    }

    private static String range(ReadyOperand operand) {
        return operand.periodStart() + "/" + operand.periodEnd();
    }

    private static Excerpt excerpt(ReadyOperand operand) {
        return new Excerpt(operand.sourceId(), operand.start(), operand.end(), operand.excerpt());
    }

    private static int ruleOrder(String ruleId) {
        IssueClass[] values = IssueClass.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].ruleId().equals(ruleId)) {
                return i;
            }
        }
        return values.length;
    }

    private static IssueClass wire(String wireName) {
        for (IssueClass issue : IssueClass.values()) {
            if (issue.wire().equals(wireName)) {
                return issue;
            }
        }
        throw new IllegalStateException(wireName);
    }

    private record Scale(boolean attempted, boolean applied, String unit, String left, String right, boolean shown) {
        static Scale skipped() {
            return new Scale(false, false, null, null, null, false);
        }

        static Scale of(ReadyOperand left, ReadyOperand right, String unit) {
            boolean applied = !left.unit().identity() || !right.unit().identity();
            return new Scale(
                    true,
                    applied,
                    unit,
                    Numbers.plain(canonical(left)),
                    Numbers.plain(canonical(right)),
                    true);
        }

        private static BigDecimal canonical(ReadyOperand operand) {
            return operand.unit().toCanonical(operand.value());
        }
    }
}
