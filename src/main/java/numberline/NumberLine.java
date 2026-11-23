package numberline;

import numberline.Model.LintReport;

/** Public entry point. Document bytes and claim JSON are the whole input. */
public final class NumberLine {
    private NumberLine() {}

    public static LintReport lint(byte[] document, byte[] claimsJson) {
        return Engine.report(ClaimReader.read(document, claimsJson));
    }
}
