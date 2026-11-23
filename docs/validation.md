# Validation

Access date: 2026-11-23. The checks below are against the synthetic fixture in this repository, not a clinical corpus and not FinQA.

## Environment

- Host: Linux 6.12.113+ x86_64, 4 CPUs
- Java: OpenJDK 21.0.13.0.3 (2026-10-24)
- Maven: 3.9.16 through the Maven Wrapper
- Fixture: `fixtures/research-report.md`, 2048 bytes, 24 comparisons

## Acceptance metric

Every row in `fixtures/oracle.json` must match the engine on verdict, issue class, normalized magnitude, and arithmetic status. Text extraction is outside the metric. Observed result from `./mvnw -B -ntp verify`:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

Counted from the report, not from a second copy of the engine:

- compatible = 10
- incompatible = 9
- unknown = 5
- issue classes: incompatible_unit 1, incompatible_currency 2, incompatible_period 3, incompatible_population 1, incompatible_numerator 1, incompatible_denominator 2, unknown_missing_period 1, unknown_missing_population 1, unknown_missing_currency 1, unknown_missing_numerator 1, unknown_missing_denominator 1

Hand factors checked in `NumbersAndUnitsTest` with `BigDecimal`, separate from the Python `decimal.Decimal` checks in `scripts/build_fixtures.py`:

- 1.2 kg = 1200 g, 1,200 g = 1200 g, 1,200,000 mg = 1200 g, including operand reversal (`C01`, `C02`, `C03`)
- 8 percent = 0.08 and 80 per_1000 = 0.08 (`C12`, `C13`)
- 8 percent of orders versus 8 percent of customers is `incompatible_denominator` even though both scales are 0.08 (`C06`)
- Removing the left denominator on `C06` changes the verdict to `unknown`
- Removing the left period on `C06` stays `unknown` and still lists the denominator conflict
- Spans in the multibyte passage (`naïve`, `患者`, `µg`) sit on code-point boundaries. `fixtures/invalid/span-split.json` is rejected as `span`
- `NaN`, `furlong`, a bad hash, and an unknown JSON property are `number`, `unit`, `hash`, and `schema`

## Domain safeguard

Outcomes come from the declared claim fields. The report sets `sourceTruthChecked` to false. Unknown stays explicit. Invalid offsets, unknown units, and nonfinite values fail before normalization. The `C11` excerpt contains `<script>alert(1)</script>` and `**bold**`. The text report contains `&lt;script&gt;` and does not contain a raw `<script` string. The JSON file escapes the tag as `\u003c` and parses back to the original excerpt. The excerpt line is indented so it is not a Markdown heading.

## Trust boundary

- Inputs are the two files named on the command line. The engine does not spawn a process, open a socket, or follow a claim URL.
- The document hash must match before spans are trusted.
- Writes go to `--output` only. The path cannot be the document or the claims file. An invalid fixture does not create that file.
- Size limits: 1 MiB per file, 256 comparisons, 64-character values, 500-byte spans.
- No credentials are read or stored. A search of the tracked tree for private keys and token assignments is part of the release check.

## Dependencies and licenses

Observed `dependency:tree` runtime closure, shaded into `target/numberline-0.1.0.jar`:

- `com.fasterxml.jackson.core:jackson-databind:2.23.0`
- `com.fasterxml.jackson.core:jackson-core:2.23.0`
- `com.fasterxml.jackson.core:jackson-annotations:2.23`

Test only: `org.junit.jupiter:junit-jupiter:6.1.3`.

OSV (`https://api.osv.dev/v1/query`) on 2026-11-23 returned no vulnerabilities for jackson-databind 2.23.0, jackson-core 2.23.0, jackson-annotations 2.23, junit-jupiter 6.1.3, junit-jupiter-api 6.1.3, or junit-jupiter-engine 6.1.3.

Jackson is Apache-2.0. JUnit is EPL-2.0 and is not shipped in the runtime jar. The project license is MIT. See `NOTICE`.

There is no native code, so an address or undefined-behavior sanitizer job does not apply. The compiler runs with `-Xlint:all,-processing,-serial` on Java 21. The verify log showed no compiler warnings.

## Performance budget

The supported demo is 24 comparisons and a 2048-byte passage. Budget: 50 in-process classifications under 2 seconds after one warmup, and the documented demo command under 15 seconds including a warm Maven package.

Observed:

- `PerformanceTest` completed in 0.073 seconds (one warmup plus 50 lints) during `./mvnw -B -ntp verify`.
- `./scripts/demo.sh` wall time was 2.147 seconds on this host with Maven already downloaded. It wrote `target/demo-report.txt`, `target/demo-report.json`, and `target/invalid-stderr.txt`.

Both budgets passed.

## Clean copy

A copy of the working tree without `target/` or `.git` was unpacked at `/tmp/nl-clean`. The README commands were:

```text
./mvnw -B -ntp verify
./scripts/demo.sh
```

`verify` reported `Tests run: 11, Failures: 0`. The demo wrote `summary: total=24 compatible=10 incompatible=9 unknown=5`, `C06 verdict=incompatible`, and `invalid_input: number: comparison C01 left value 'NaN' is not a finite decimal`. `java -jar target/numberline-0.1.0.jar version` printed `numberline 0.1.0`. A tracked-file search for private-key blocks, AWS access-key ids, and token assignments found no matches.

## Residual limits

Exchange rates, inflation, unlisted units, and fuzzy wording are out of scope and are stated in the report limitations. The metric does not score free-text extraction. Hosted CI for this commit is recorded in P07.
