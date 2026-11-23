package numberline;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Arrays;
import numberline.Model.BasisDifference;
import numberline.Model.ComparisonResult;
import numberline.Model.Finding;
import numberline.Model.LintReport;

/** Text for a reviewer and JSON for CI. Both renderings come from the same report object. */
public final class Reports {
    private static final JsonMapper JSON = jsonMapper();

    private static JsonMapper jsonMapper() {
        JsonFactory factory = new JsonFactory();
        factory.setCharacterEscapes(new HtmlSafeEscapes());
        return JsonMapper.builder(factory).enable(SerializationFeature.INDENT_OUTPUT).build();
    }

    private Reports() {}

    public static String json(LintReport report) {
        try {
            return JSON.writeValueAsString(report) + "\n";
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("could not write the JSON report", ex);
        }
    }

    public static String text(LintReport report) {
        StringBuilder out = new StringBuilder();
        out.append("NumberLine ").append(report.tool().version()).append('\n');
        out.append("schemaVersion: ").append(report.schemaVersion()).append('\n');
        out.append("document: ").append(escape(report.document().id())).append('\n');
        out.append("sha256: ").append(report.document().sha256()).append('\n');
        out.append("bytes: ").append(report.document().byteLength()).append('\n');
        out.append("summary: total=")
                .append(report.summary().total())
                .append(" compatible=")
                .append(report.summary().compatible())
                .append(" incompatible=")
                .append(report.summary().incompatible())
                .append(" unknown=")
                .append(report.summary().unknown())
                .append('\n');
        report.summary().byIssueClass().forEach((name, count) -> out.append("issueClass ")
                .append(name)
                .append('=')
                .append(count)
                .append('\n'));
        out.append("provenance: ").append(escape(report.comparisons().isEmpty()
                ? Engine.PROVENANCE
                : report.comparisons().get(0).provenance())).append('\n');
        out.append("limitations:\n");
        for (String limitation : report.limitations()) {
            out.append("    - ").append(escape(limitation)).append('\n');
        }
        out.append('\n');
        for (ComparisonResult result : report.comparisons()) {
            appendComparison(out, result);
        }
        return out.toString();
    }

    private static void appendComparison(StringBuilder out, ComparisonResult result) {
        out.append(result.id()).append(" verdict=").append(result.verdict()).append('\n');
        out.append("rules:");
        for (Finding finding : result.findings()) {
            out.append(' ').append(finding.ruleId());
        }
        out.append('\n');
        out.append("issues: ");
        out.append(result.issueClasses().isEmpty() ? "none" : String.join(" ", result.issueClasses()));
        out.append('\n');
        out.append("missing: ");
        out.append(result.missingFields().isEmpty() ? "none" : String.join(" ", result.missingFields()));
        out.append('\n');
        if (result.basisDifferences().isEmpty()) {
            out.append("basis: none\n");
        } else {
            for (BasisDifference difference : result.basisDifferences()) {
                out.append("basis ")
                        .append(difference.field())
                        .append(" rule=")
                        .append(difference.ruleId())
                        .append(" left=\"")
                        .append(escape(difference.left()))
                        .append("\" right=\"")
                        .append(escape(difference.right()))
                        .append("\"\n");
            }
        }
        out.append("conversion: attempted=")
                .append(result.conversion().attempted())
                .append(" applied=")
                .append(result.conversion().applied())
                .append(" canonicalUnit=")
                .append(result.conversion().canonicalUnit() == null ? "none" : result.conversion().canonicalUnit())
                .append('\n');
        out.append("normalized: status=")
                .append(result.normalized().status())
                .append(" left=")
                .append(result.normalized().left() == null ? "none" : result.normalized().left())
                .append(" right=")
                .append(result.normalized().right() == null ? "none" : result.normalized().right())
                .append(" unit=")
                .append(result.normalized().unit() == null ? "none" : result.normalized().unit())
                .append('\n');
        out.append("arithmetic: ").append(result.arithmetic().status());
        if (result.arithmetic().reason() != null) {
            out.append(" reason=").append(result.arithmetic().reason());
        }
        out.append('\n');
        out.append("    excerpt left source=")
                .append(escape(result.excerpts().left().sourceId()))
                .append(" bytes=[")
                .append(result.excerpts().left().start())
                .append(',')
                .append(result.excerpts().left().end())
                .append(") text=\"")
                .append(escape(result.excerpts().left().text()))
                .append("\"\n");
        out.append("    excerpt right source=")
                .append(escape(result.excerpts().right().sourceId()))
                .append(" bytes=[")
                .append(result.excerpts().right().start())
                .append(',')
                .append(result.excerpts().right().end())
                .append(") text=\"")
                .append(escape(result.excerpts().right().text()))
                .append("\"\n");
        for (Finding finding : result.findings()) {
            out.append("    message ")
                    .append(finding.ruleId())
                    .append(": ")
                    .append(escape(finding.message()))
                    .append('\n');
        }
        out.append('\n');
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            switch (codePoint) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (codePoint < 0x20) {
                        out.append(String.format("\\u%04x", codePoint));
                    } else {
                        out.appendCodePoint(codePoint);
                    }
                }
            }
        }
        return out.toString();
    }

    /** JSON escapes that keep `<`, `>`, and `&` from appearing raw in a report file. */
    static final class HtmlSafeEscapes extends CharacterEscapes {
        private final int[] ascii;
        private static final SerializedString LT = new SerializedString("\\u003c");
        private static final SerializedString GT = new SerializedString("\\u003e");
        private static final SerializedString AMP = new SerializedString("\\u0026");

        HtmlSafeEscapes() {
            int[] standard = standardAsciiEscapesForJSON();
            ascii = Arrays.copyOf(standard, standard.length);
            ascii['<'] = ESCAPE_CUSTOM;
            ascii['>'] = ESCAPE_CUSTOM;
            ascii['&'] = ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return ascii;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return switch (ch) {
                case '<' -> LT;
                case '>' -> GT;
                case '&' -> AMP;
                default -> null;
            };
        }
    }
}
