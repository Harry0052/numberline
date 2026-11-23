package numberline;

/** A fixture or claim file failed a check that is not a compatibility classification. */
public final class InvalidInputException extends RuntimeException {
    private final String category;

    public InvalidInputException(String category, String message) {
        super(message);
        this.category = category;
    }

    public String category() {
        return category;
    }

    public String diagnostic() {
        return "invalid_input: " + category + ": " + getMessage();
    }
}
