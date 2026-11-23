package numberline;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** UTF-8 byte spans. A span is valid only when both ends fall on code-point boundaries. */
public final class Spans {
    private Spans() {}

    public static boolean isBoundary(byte[] utf8, int index) {
        if (index < 0 || index > utf8.length) {
            return false;
        }
        if (index == 0 || index == utf8.length) {
            return true;
        }
        return (utf8[index] & 0xC0) != 0x80;
    }

    public static String excerpt(byte[] utf8, int start, int end) {
        if (start < 0 || end < start || end > utf8.length) {
            throw new InvalidInputException("span", "span [" + start + "," + end + ") is outside the document");
        }
        if (!isBoundary(utf8, start)) {
            throw new InvalidInputException(
                    "span", "span [" + start + "," + end + ") splits a UTF-8 code point at offset " + start);
        }
        if (!isBoundary(utf8, end)) {
            throw new InvalidInputException(
                    "span", "span [" + start + "," + end + ") splits a UTF-8 code point at offset " + end);
        }
        if (end - start > Limits.MAX_SPAN_BYTES) {
            throw new InvalidInputException(
                    "limit", "span [" + start + "," + end + ") exceeds " + Limits.MAX_SPAN_BYTES + " bytes");
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(utf8, start, end - start)).toString();
        } catch (CharacterCodingException ex) {
            throw new InvalidInputException(
                    "span", "span [" + start + "," + end + ") is not valid UTF-8");
        }
    }
}
