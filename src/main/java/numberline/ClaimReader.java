package numberline;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import numberline.Model.ReadyComparison;
import numberline.Model.ReadyInput;
import numberline.Model.ReadyOperand;

/** Strict claim JSON. Missing keys are schema errors. JSON null is an omitted basis field. */
public final class ClaimReader {
    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern ID = Pattern.compile("[A-Za-z][A-Za-z0-9._:-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .build();

    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "document", "comparisons");
    private static final Set<String> DOCUMENT_KEYS = Set.of("id", "sha256", "encoding");
    private static final Set<String> COMPARISON_KEYS = Set.of("id", "operation", "left", "right");
    private static final Set<String> OPERAND_KEYS = Set.of(
            "sourceId",
            "span",
            "value",
            "unit",
            "currency",
            "periodStart",
            "periodEnd",
            "numerator",
            "denominator",
            "populationKey");
    private static final Set<String> SPAN_KEYS = Set.of("start", "end");

    private ClaimReader() {}

    public static ReadyInput read(byte[] document, byte[] claimsJson) {
        if (document == null || claimsJson == null) {
            throw new InvalidInputException("schema", "document and claims are required");
        }
        if (document.length > Limits.MAX_DOCUMENT_BYTES) {
            throw new InvalidInputException(
                    "limit",
                    "document is "
                            + document.length
                            + " bytes; the limit is "
                            + Limits.MAX_DOCUMENT_BYTES
                            + " bytes");
        }
        if (claimsJson.length > Limits.MAX_CLAIMS_BYTES) {
            throw new InvalidInputException(
                    "limit",
                    "claims JSON is "
                            + claimsJson.length
                            + " bytes; the limit is "
                            + Limits.MAX_CLAIMS_BYTES
                            + " bytes");
        }
        JsonNode root = parse(claimsJson);
        expectKeys(root, ROOT_KEYS, "claims");
        if (!root.isObject()) {
            throw schema("claims must be a JSON object");
        }
        String schemaVersion = requiredString(root, "claims", "schemaVersion");
        if (!"1".equals(schemaVersion)) {
            throw schema("schemaVersion must be \"1\"");
        }
        JsonNode documentNode = requiredObject(root, "claims", "document");
        expectKeys(documentNode, DOCUMENT_KEYS, "document");
        String documentId = requiredId(documentNode, "document", "id");
        String declaredHash = requiredString(documentNode, "document", "sha256");
        if (!SHA256.matcher(declaredHash).matches()) {
            throw schema("document.sha256 must be 64 lowercase hex characters");
        }
        String encoding = requiredString(documentNode, "document", "encoding");
        if (!"UTF-8".equals(encoding)) {
            throw schema("document.encoding must be \"UTF-8\"");
        }
        String actualHash = HexFormat.of().formatHex(sha256(document));
        if (!actualHash.equals(declaredHash)) {
            throw new InvalidInputException(
                    "hash", "document sha256 does not match claims.document.sha256");
        }
        JsonNode comparisons = requiredArray(root, "claims", "comparisons");
        if (comparisons.size() > Limits.MAX_COMPARISONS) {
            throw new InvalidInputException(
                    "limit",
                    "comparisons has "
                            + comparisons.size()
                            + " rows; the limit is "
                            + Limits.MAX_COMPARISONS);
        }
        List<ReadyComparison> ready = new ArrayList<>();
        Set<String> comparisonIds = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        int index = 0;
        for (JsonNode comparison : comparisons) {
            String path = "comparisons[" + index + "]";
            if (!comparison.isObject()) {
                throw schema(path + " must be an object");
            }
            expectKeys(comparison, COMPARISON_KEYS, path);
            String id = requiredId(comparison, path, "id");
            if (!comparisonIds.add(id)) {
                throw schema("duplicate comparison id '" + id + "'");
            }
            String operation = requiredString(comparison, path, "operation");
            if (!"compare".equals(operation)) {
                throw schema(path + ".operation must be \"compare\"");
            }
            ReadyOperand left = operand(comparison, id, "left", document, sourceIds);
            ReadyOperand right = operand(comparison, id, "right", document, sourceIds);
            ready.add(new ReadyComparison(id, left, right));
            index++;
        }
        return new ReadyInput(documentId, actualHash, document, List.copyOf(ready));
    }

    private static ReadyOperand operand(
            JsonNode comparison, String id, String side, byte[] document, Set<String> sourceIds) {
        String path = "comparison " + id + " " + side;
        JsonNode node = requiredObject(comparison, "comparison " + id, side);
        expectKeys(node, OPERAND_KEYS, path);
        String sourceId = requiredId(node, path, "sourceId");
        if (!sourceIds.add(sourceId)) {
            throw schema("duplicate sourceId '" + sourceId + "'");
        }
        JsonNode span = requiredObject(node, path, "span");
        expectKeys(span, SPAN_KEYS, path + ".span");
        int start = requiredInt(span, path + ".span", "start");
        int end = requiredInt(span, path + ".span", "end");
        if (end < start) {
            throw new InvalidInputException("span", path + " span end is before start");
        }
        String excerpt;
        try {
            excerpt = Spans.excerpt(document, start, end);
        } catch (InvalidInputException ex) {
            throw new InvalidInputException(ex.category(), path + " " + ex.getMessage());
        }
        String valueText = requiredString(node, path, "value");
        if (valueText.length() > Limits.MAX_VALUE_CHARS) {
            throw new InvalidInputException(
                    "limit", path + " value exceeds " + Limits.MAX_VALUE_CHARS + " characters");
        }
        java.math.BigDecimal value;
        try {
            value = Numbers.parse(valueText);
        } catch (InvalidInputException ex) {
            throw new InvalidInputException("number", path + " " + ex.getMessage());
        }
        String unitId = requiredString(node, path, "unit");
        Units.UnitDef unit;
        try {
            unit = Units.require(unitId);
        } catch (InvalidInputException ex) {
            throw new InvalidInputException("unit", path + " " + ex.getMessage());
        }
        String currency = optionalText(node, path, "currency");
        if (currency != null) {
            try {
                Units.requireCurrency(currency);
            } catch (InvalidInputException ex) {
                throw new InvalidInputException("unit", path + " " + ex.getMessage());
            }
            if (unit.dimension() != Units.Dimension.MONEY) {
                throw new InvalidInputException("unit", path + " currency is only valid when unit is money");
            }
        }
        LocalDate periodStart = optionalDate(node, path, "periodStart");
        LocalDate periodEnd = optionalDate(node, path, "periodEnd");
        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw schema(path + " periodEnd is before periodStart");
        }
        if ((periodStart == null) != (periodEnd == null)) {
            throw schema(path + " periodStart and periodEnd must both be present or both be null");
        }
        String numerator = optionalText(node, path, "numerator");
        String denominator = optionalText(node, path, "denominator");
        String populationKey = optionalText(node, path, "populationKey");
        return new ReadyOperand(
                sourceId,
                start,
                end,
                excerpt,
                value,
                unit,
                currency,
                periodStart,
                periodEnd,
                numerator,
                denominator,
                populationKey);
    }

    private static JsonNode parse(byte[] claimsJson) {
        JsonFactory factory = MAPPER.getFactory();
        try (JsonParser parser = factory.createParser(claimsJson)) {
            JsonNode node = MAPPER.readTree(parser);
            if (node == null) {
                throw schema("claims JSON is empty");
            }
            if (parser.nextToken() != null) {
                throw schema("claims JSON has trailing content");
            }
            return node;
        } catch (InvalidInputException ex) {
            throw ex;
        } catch (JsonProcessingException ex) {
            throw schema("claims JSON is not valid");
        } catch (java.io.IOException ex) {
            throw schema("claims JSON is not valid");
        }
    }

    private static void expectKeys(JsonNode node, Set<String> allowed, String path) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                throw schema(path + " property '" + name + "' is not supported");
            }
        }
        for (String required : allowed) {
            if (!node.has(required)) {
                throw schema(path + " is missing property '" + required + "'");
            }
        }
    }

    private static JsonNode requiredObject(JsonNode parent, String path, String field) {
        if (!parent.has(field)) {
            throw schema(path + " is missing property '" + field + "'");
        }
        JsonNode node = parent.get(field);
        if (!node.isObject()) {
            throw schema(path + "." + field + " must be an object");
        }
        return node;
    }

    private static JsonNode requiredArray(JsonNode parent, String path, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isArray()) {
            throw schema(path + "." + field + " must be an array");
        }
        return node;
    }

    private static String requiredString(JsonNode parent, String path, String field) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull() || !node.isTextual()) {
            throw schema(path + "." + field + " must be a string");
        }
        String value = node.textValue();
        if (value.isBlank()) {
            throw schema(path + "." + field + " must not be blank");
        }
        if (containsControl(value)) {
            throw schema(path + "." + field + " contains a control character");
        }
        return value;
    }

    private static String requiredId(JsonNode parent, String path, String field) {
        String value = requiredString(parent, path, field);
        if (value.length() > Limits.MAX_ID_CHARS || !ID.matcher(value).matches()) {
            throw schema(path + "." + field + " must be an identifier of at most " + Limits.MAX_ID_CHARS + " characters");
        }
        return value;
    }

    private static String optionalText(JsonNode parent, String path, String field) {
        if (!parent.has(field)) {
            throw schema(path + " is missing property '" + field + "'");
        }
        JsonNode node = parent.get(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw schema(path + "." + field + " must be a string or null");
        }
        String value = node.textValue();
        if (value.isBlank()) {
            throw schema(path + "." + field + " must not be blank");
        }
        if (value.length() > Limits.MAX_TEXT_CHARS) {
            throw new InvalidInputException(
                    "limit", path + "." + field + " exceeds " + Limits.MAX_TEXT_CHARS + " characters");
        }
        if (containsControl(value)) {
            throw schema(path + "." + field + " contains a control character");
        }
        return value;
    }

    private static LocalDate optionalDate(JsonNode parent, String path, String field) {
        String text = optionalText(parent, path, field);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text, ISO_DATE);
        } catch (DateTimeParseException ex) {
            throw schema(path + "." + field + " '" + text + "' is not an ISO-8601 date");
        }
    }

    private static int requiredInt(JsonNode parent, String path, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isIntegralNumber() || node.isFloat() || !node.canConvertToInt()) {
            throw schema(path + "." + field + " must be an integer");
        }
        return node.intValue();
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(cp -> cp < 0x20 || cp == 0x7F);
    }

    private static byte[] sha256(byte[] document) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(document);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    private static InvalidInputException schema(String message) {
        return new InvalidInputException("schema", message);
    }
}
