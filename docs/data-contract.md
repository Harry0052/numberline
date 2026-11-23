# Data contract, schema 1

NumberLine reads two local inputs and writes one report. Nothing is fetched at run time.

## Claim file

`schemaVersion` is `"1"`. Unknown JSON properties are rejected. Every operand key must be present. A JSON `null` means the basis field was omitted. A missing key is invalid input, not `unknown`.

Document object:

| Field | Rule |
| --- | --- |
| `id` | Identifier, at most 64 characters |
| `sha256` | Lowercase hex SHA-256 of the Markdown file bytes |
| `encoding` | Exactly `UTF-8` |

Each comparison has `id`, `operation` (`compare` only), `left`, and `right`.

Each operand:

| Field | Rule |
| --- | --- |
| `sourceId` | Unique identifier |
| `span.start`, `span.end` | UTF-8 byte offsets, `end >= start`, both on code-point boundaries, inside the document, at most 500 bytes |
| `value` | String, at most 64 characters. Finite decimal. Optional thousands commas. No exponent |
| `unit` | `mg`, `g`, `kg`, `percent`, `per_100`, `per_1000`, `proportion`, `money`, or `count` |
| `currency` | `null`, or `USD` or `EUR` when `unit` is `money` |
| `periodStart`, `periodEnd` | Both `null`, or both real `YYYY-MM-DD` dates with end on or after start |
| `numerator`, `denominator`, `populationKey` | `null` or a short text without control characters |

Bounds: each file at most 1 MiB, at most 256 comparisons.

## Classifications

`compatible`, `incompatible`, and `unknown` are the metric. Invalid input is a separate diagnostic, `invalid_input: <category>: <message>`, with category `schema`, `hash`, `span`, `number`, `unit`, `limit`, or `io`.

Required on every comparison: period and population. Required for money: currency. Required when both sides are rates: numerator and denominator. A missing required field is `unknown`. A present mismatch is `incompatible`. Issue classes are:

- `incompatible_unit`
- `incompatible_currency`
- `incompatible_period`
- `incompatible_population`
- `incompatible_numerator`
- `incompatible_denominator`
- `unknown_missing_period`
- `unknown_missing_population`
- `unknown_missing_currency`
- `unknown_missing_numerator`
- `unknown_missing_denominator`

If any required field is missing, the verdict is `unknown` even when another present field also conflicts. Both the missing field and the conflict are listed.

Mass canonical unit is grams: `mg = 1/1000`, `g = 1`, `kg = 1000`. Rate canonical unit is a proportion: `percent = 1/100`, `per_100 = 1/100`, `per_1000 = 1/1000`, `proportion = 1`. Money and counts use the parsed number. No exchange rate and no inflation factor exist. Magnitudes are marked equal or mismatched only when the verdict is `compatible`.

## Report

`schemaVersion` is `"1"`. Text and JSON come from the same object. Findings use stable rule ids (`NL-DENOMINATOR`, `NL-PERIOD`, `NL-CURRENCY`, `NL-COMPATIBLE`, `NL-CONVERSION-APPLIED`, `NL-ARITHMETIC-EQUAL`, and the matching ids for the other cases). Excerpts are the decoded span text. The text report HTML-escapes them. The JSON writer escapes `<`, `>`, and `&`. `sourceTruthChecked` is false.
