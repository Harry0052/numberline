package numberline;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Decimal values. Grouping commas are allowed. Exponents and nonfinite tokens are not. */
public final class Numbers {
    private static final Pattern DECIMAL =
            Pattern.compile("[+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?");

    private Numbers() {}

    public static BigDecimal parse(String raw) {
        if (raw == null) {
            throw new InvalidInputException("number", "value is missing");
        }
        String text = raw.trim();
        if (text.equalsIgnoreCase("NaN")
                || text.equalsIgnoreCase("Infinity")
                || text.equalsIgnoreCase("+Infinity")
                || text.equalsIgnoreCase("-Infinity")) {
            throw new InvalidInputException("number", "value '" + text + "' is not a finite decimal");
        }
        if (!DECIMAL.matcher(text).matches()) {
            throw new InvalidInputException("number", "value '" + text + "' is not a finite decimal");
        }
        String normalized = text.replace(",", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new InvalidInputException("number", "value '" + text + "' is not a finite decimal");
        }
    }

    public static String plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }
}
