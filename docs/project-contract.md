# NumberLine project contract

Status: accepted for implementation after the P01 overlap review on 2026-11-23.

## User and task

A methods reviewer loads one annotated Markdown passage and one structured claim file. NumberLine classifies each binary comparison as `compatible`, `incompatible`, or `unknown` from the declared basis fields. It shows normalized values when the units share a dimension, the exact UTF-8 byte span of each side, and the reason for every warning.

The reviewer confirms the structured extraction before the run. The tool does not decide whether the passage is true, and it does not repair a number.

## Minimum dependencies

- Java 21
- Apache Maven Wrapper, pinned to a stable Maven 3.9 distribution, for build and test
- Jackson, for strict JSON input and JSON reports, pinned in the Maven POM
- JUnit, test scope only

Not in the MVP runtime:

- Indriya or any other units library. Conversion factors live in a tested allowlist.
- Network access, model APIs, exchange-rate services, or a downloaded benchmark corpus
- Paid services and credentials

Fixtures are original text covered by the repository MIT license. They load from the working tree.

## Acceptance case

Fixture rows A and B from `docs/overlap.md` are the gate for the core:

1. `1.2 kg` and `1,200 g`, with the same period, population, and no currency, classify as `compatible`. Canonical magnitudes are equal. Swapping the operands keeps them equal.
2. `8` percent of orders and `8` percent of customers, over the same month, classify as `incompatible` with issue class `incompatible_denominator`.
3. Removing one required basis field from that second comparison changes the classification to `unknown`.

The release metric extends that case to a frozen 24-row fixture. Every row must receive the correct `compatible`, `incompatible`, or `unknown` classification, reported by issue class. Text extraction is outside the metric. Invalid files must fail with a diagnostic that is not one of those three classifications.

## Boundaries carried forward

- Allowlist only: milligram, gram, kilogram; percent, per 100, per 1,000, and proportion; the money unit with an explicit currency code; count.
- Same-currency amounts only. No exchange rates and no inflation adjustment across years.
- A rate keeps its numerator definition, denominator definition, and population key. Those fields are not implied by a dimensionless scale.
- Period, population, and currency must match when they are relevant. The only supported conversion is an allowlisted unit factor.
- Missing essential basis metadata is `unknown`. Malformed JSON, a span that splits a code point, an unknown unit, or a nonfinite value is invalid input.
- Compatibility is reported separately from whether the normalized magnitudes are equal.
