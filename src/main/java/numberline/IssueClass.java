package numberline;

/** Stable classification reasons. Wire names are the acceptance-metric labels. */
public enum IssueClass {
    INCOMPATIBLE_UNIT("incompatible_unit", "NL-UNIT-DIMENSION", "unit"),
    INCOMPATIBLE_CURRENCY("incompatible_currency", "NL-CURRENCY", "currency"),
    INCOMPATIBLE_PERIOD("incompatible_period", "NL-PERIOD", "period"),
    INCOMPATIBLE_POPULATION("incompatible_population", "NL-POPULATION", "population"),
    INCOMPATIBLE_NUMERATOR("incompatible_numerator", "NL-NUMERATOR", "numerator"),
    INCOMPATIBLE_DENOMINATOR("incompatible_denominator", "NL-DENOMINATOR", "denominator"),
    UNKNOWN_MISSING_PERIOD("unknown_missing_period", "NL-MISSING-PERIOD", "period"),
    UNKNOWN_MISSING_POPULATION("unknown_missing_population", "NL-MISSING-POPULATION", "population"),
    UNKNOWN_MISSING_CURRENCY("unknown_missing_currency", "NL-MISSING-CURRENCY", "currency"),
    UNKNOWN_MISSING_NUMERATOR("unknown_missing_numerator", "NL-MISSING-NUMERATOR", "numerator"),
    UNKNOWN_MISSING_DENOMINATOR("unknown_missing_denominator", "NL-MISSING-DENOMINATOR", "denominator");

    private final String wire;
    private final String ruleId;
    private final String field;

    IssueClass(String wire, String ruleId, String field) {
        this.wire = wire;
        this.ruleId = ruleId;
        this.field = field;
    }

    public String wire() {
        return wire;
    }

    public String ruleId() {
        return ruleId;
    }

    public String field() {
        return field;
    }
}
