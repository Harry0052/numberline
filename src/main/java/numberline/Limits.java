package numberline;

/** Resource bounds for one local lint. Sizes are deliberate, not inferred from the sample. */
public final class Limits {
    public static final int MAX_DOCUMENT_BYTES = 1_048_576;
    public static final int MAX_CLAIMS_BYTES = 1_048_576;
    public static final int MAX_COMPARISONS = 256;
    public static final int MAX_VALUE_CHARS = 64;
    public static final int MAX_ID_CHARS = 64;
    public static final int MAX_TEXT_CHARS = 200;
    public static final int MAX_SPAN_BYTES = 500;

    private Limits() {}
}
