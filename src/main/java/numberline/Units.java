package numberline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exact allowlist. Factors are rational and terminating. Currency identity is not a factor:
 * USD and EUR are recognized codes with no exchange rate between them.
 */
public final class Units {
    public enum Dimension {
        MASS,
        RATE,
        MONEY,
        COUNT
    }

    public record UnitDef(String id, Dimension dimension, BigDecimal multiply, BigDecimal divide, boolean identity) {
        public BigDecimal toCanonical(BigDecimal value) {
            return value.multiply(multiply).divide(divide);
        }

        public String canonicalName(String currency) {
            return switch (dimension) {
                case MASS -> "g";
                case RATE -> "proportion";
                case COUNT -> "count";
                case MONEY -> currency == null ? "money" : currency;
            };
        }
    }

    public static final List<String> IDS =
            List.of("mg", "g", "kg", "percent", "per_100", "per_1000", "proportion", "money", "count");
    public static final List<String> CURRENCIES = List.of("USD", "EUR");

    private static final Map<String, UnitDef> BY_ID = Map.of(
            "mg", def("mg", Dimension.MASS, "1", "1000", false),
            "g", def("g", Dimension.MASS, "1", "1", true),
            "kg", def("kg", Dimension.MASS, "1000", "1", false),
            "percent", def("percent", Dimension.RATE, "1", "100", false),
            "per_100", def("per_100", Dimension.RATE, "1", "100", false),
            "per_1000", def("per_1000", Dimension.RATE, "1", "1000", false),
            "proportion", def("proportion", Dimension.RATE, "1", "1", true),
            "money", def("money", Dimension.MONEY, "1", "1", true),
            "count", def("count", Dimension.COUNT, "1", "1", true));

    private Units() {}

    public static UnitDef require(String id) {
        UnitDef unit = BY_ID.get(id);
        if (unit == null) {
            throw new InvalidInputException(
                    "unit",
                    "unit '" + id + "' is not in the allowlist (" + String.join(", ", IDS) + ")");
        }
        return unit;
    }

    public static void requireCurrency(String currency) {
        if (!CURRENCIES.contains(currency)) {
            throw new InvalidInputException(
                    "unit",
                    "currency '"
                            + currency
                            + "' is not in the allowlist ("
                            + String.join(", ", CURRENCIES)
                            + "); exchange rates are not supported");
        }
    }

    public static String allowlistText() {
        return String.join(", ", IDS);
    }

    private static UnitDef def(String id, Dimension dimension, String multiply, String divide, boolean identity) {
        return new UnitDef(id, dimension, new BigDecimal(multiply), new BigDecimal(divide), identity);
    }

    /** Display helper for messages. Not a conversion. */
    public static String dimensionName(Dimension dimension) {
        return dimension.name().toLowerCase(Locale.ROOT);
    }
}
