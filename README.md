# NumberLine

NumberLine flags numerical comparisons that use incompatible units, denominators, currencies, or reporting periods.

A methods reviewer supplies an annotated Markdown passage and a structured claim file. The tool classifies each binary comparison as `compatible`, `incompatible`, or `unknown` from the declared basis fields. It shows normalized values when a conversion is allowed, the exact UTF-8 byte span of each side, and a stable rule id for every finding. It does not decide whether the passage is true.

## Prerequisites

- Java 21
- A shell that can run the Maven Wrapper (`mvnw`)

The wrapper downloads Maven 3.9.16 from Maven Central on the first run. No other install step and no network call at lint time are required. The demo does not use a paid service.

## Install and test

```sh
./mvnw -B -ntp verify
```

`verify` compiles the project and runs the unit tests, including the frozen 24-case fixture.

## Demo

From a checkout, after the command above or on its own:

```sh
./scripts/demo.sh
```

That packages `target/numberline-0.1.0.jar`, writes the reports, and runs one invalid file. The same lint without the script is:

```sh
./mvnw -B -ntp -DskipTests package
java -jar target/numberline-0.1.0.jar lint \
  --document fixtures/research-report.md \
  --claims fixtures/claims.json \
  --format text \
  --output target/demo-report.txt
```

JSON for CI uses `--format json --output target/demo-report.json`.

### Expected result

The text report contains:

```text
summary: total=24 compatible=10 incompatible=9 unknown=5
```

`C01` is compatible: `1.2 kg` and `1,200 g` normalize to `1200 g`. `C06` is incompatible: both sides are `8` percent, and the denominators are orders versus customers. Reports are written to:

- `target/demo-report.txt`
- `target/demo-report.json`

The invalid sample `fixtures/invalid/nonfinite.json` exits `2` and prints `invalid_input: number:` on the error stream. It does not write a report.

Exit codes: `0` report written, `1` usage or missing file, `2` invalid input, `3` with `--fail-on findings` when any comparison is not compatible. The default demo exits `0` because incompatible and unknown rows are successful classifications. Diagnostics stay on the error stream.

`numberline help` lists the options.

## What the reviewer sees

Each comparison names the basis fields that differ, whether conversion was attempted, and which metadata is still required. A change to a denominator, population, period, or currency is a finding with a rule id, in the same report as a change to a number. Arithmetic equality is reported only after the basis is compatible. `C04` is the example: the units match after conversion, and the magnitudes do not.

Trace one finding back to the passage: in `target/demo-report.txt`, the `C06` lines cite rule `NL-DENOMINATOR`, the two denominator phrases, and the byte spans. Those spans slice `fixtures/research-report.md`.

## Limits

- Units: `mg`, `g`, `kg`, `percent`, `per_100`, `per_1000`, `proportion`, `money`, `count`.
- Currencies: `USD` and `EUR`, with no exchange rate.
- Periods are exact ISO-8601 dates. Unequal windows are not prorated, and different years are not inflation-adjusted.
- A rate keeps its numerator, denominator, and population. Those do not collapse into a dimensionless percent.
- Missing required metadata is `unknown`. Malformed JSON, a bad hash, a span that splits a code point, an unknown unit, or a nonfinite value is invalid input.
- Document and claims files are limited to 1 MiB and 256 comparisons.
- There is no separate model or hardware demo.

## Fixtures

`fixtures/research-report.md` and `fixtures/claims.json` are original text covered by the repository MIT license. They are not a clinical record and not a financial filing. Checksums and the regeneration command are in `fixtures/MANIFEST.md`. The hand calculations are in `fixtures/oracle.md`. Regenerate with `python3 scripts/build_fixtures.py` if you edit the passage; the script does not download data.

## License and reports

The project license is [MIT](LICENSE). Third-party notices are in [NOTICE](NOTICE). Security reports go through [SECURITY.md](SECURITY.md). See [CONTRIBUTING.md](CONTRIBUTING.md) for tests and commits. The version history is [CHANGELOG.md](CHANGELOG.md).
