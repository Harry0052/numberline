# Grokbot progress

## P06 — Prepare a reproducible open-source release

**Phase and status:** P06 PASS

**Files changed:**

- `README.md`
- `NOTICE`
- `SECURITY.md`
- `CONTRIBUTING.md`
- `CHANGELOG.md`
- `.github/workflows/ci.yml`
- `docs/validation.md`
- `docs/grokbot-progress.md`

**Commands and results:**

- Clean copy at `/tmp/nl-clean`, without `target/` or `.git`: `./mvnw -B -ntp verify` then `./scripts/demo.sh`.
- Verify: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`.
- Demo report line: `summary: total=24 compatible=10 incompatible=9 unknown=5`.
- `C06 verdict=incompatible`. Invalid run exit behavior checked by the script: stderr `invalid_input: number: comparison C01 left value 'NaN' is not a finite decimal`, and no `target/invalid-report.txt`.
- `java -jar target/numberline-0.1.0.jar version` printed `numberline 0.1.0`.
- Tracked-file search for private-key blocks, AWS access-key ids, and token assignments: no matches.
- Workflow `.github/workflows/ci.yml` uses `contents: read` plus `actions: write` only for the artifact upload. Actions are pinned to commit SHAs: checkout `3d3c42e5aac5ba805825da76410c181273ba90b1`, setup-java `d1374f8ac74474200d16ef8ecee22c7ae7a51880`, upload-artifact `cf430e030ddbb5b0abf93d22962f4752f3646cd9`. No secrets.

**Artifact paths:**

- `target/numberline-0.1.0.jar` (shaded, not committed)
- `target/demo-report.txt`
- `target/demo-report.json`
- `target/invalid-stderr.txt`

**Open issues:**

- Hosted GitHub Actions has not run yet. That is the P07 gate.

**Next phase:** P07

## P05 — Verify correctness, security, and performance

**Phase and status:** P05 PASS

**Files changed:**

- `docs/validation.md`
- `src/test/java/numberline/PerformanceTest.java`
- `src/test/java/numberline/FixtureTest.java` (span, markup, and limit assertions)
- `pom.xml` (shade so the jar runs; compiler lint)

**Commands and results:**

- `./mvnw -B -ntp verify`: 11 tests, 0 failures. `PerformanceTest` elapsed 0.073 seconds.
- OSV query on 2026-11-23: no vulnerabilities for jackson-databind 2.23.0, jackson-core 2.23.0, jackson-annotations 2.23, or JUnit Jupiter 6.1.3.
- `./scripts/demo.sh` wall time 2.147 seconds on Linux 6.12.113+ x86_64, 4 CPUs, OpenJDK 21.0.13.0.3. Budget is 15 seconds for the demo and 2 seconds for 50 in-process lints. Both passed.
- Markup safeguard: text report contains `&lt;script&gt;` and the JSON bytes contain `\u003c`, not a raw `<script` string.

**Artifact paths:**

- `docs/validation.md`
- `target/demo-report.txt`

**Open issues:**

- None in the local safeguard. Exchange rates and free-text extraction stay out of scope on purpose.

**Next phase:** P06

## P04 — Ship the user workflow and reports

**Phase and status:** P04 PASS

**Files changed:**

- `src/main/java/numberline/Main.java`
- `src/main/java/numberline/Reports.java`
- `scripts/demo.sh`
- `README.md`
- `src/test/java/numberline/CliTest.java`

**Commands and results:**

- `./scripts/demo.sh` wrote `target/demo-report.txt` and `target/demo-report.json`.
- Summary: `total=24 compatible=10 incompatible=9 unknown=5`.
- `C06` is `verdict=incompatible` with rule `NL-DENOMINATOR`, and the excerpt spans point at the passage.
- Invalid path: `fixtures/invalid/nonfinite.json` prints `invalid_input: number:` and does not create the output file. CLI test exit codes: help 0, missing arguments 1, invalid input 2, `--fail-on findings` 3.

**Artifact paths:**

- `target/demo-report.txt`
- `target/demo-report.json`
- `target/invalid-stderr.txt`

**Open issues:**

- None for the local workflow.

**Next phase:** P05

## P03 — Build the working core

**Phase and status:** P03 PASS

**Files changed:**

- `src/main/java/numberline/Engine.java`
- `src/main/java/numberline/Numbers.java`
- `src/main/java/numberline/Units.java`
- `src/main/java/numberline/NumberLine.java`
- `src/test/java/numberline/FixtureTest.java`
- `src/test/java/numberline/NumbersAndUnitsTest.java`
- `fixtures/oracle.json`
- `fixtures/oracle.md`

**Commands and results:**

- `./mvnw -B -ntp verify` oracle and proof tests passed.
- `C01`: compatible, both canonical magnitudes `1200` grams. `C02` reverses the operands and stays equal.
- `C06`: incompatible, issue class `incompatible_denominator`, both scales `0.08`, arithmetic `not_evaluated`.
- Setting the left denominator of `C06` to null changes the verdict to `unknown`.
- All 24 rows match `fixtures/oracle.json`.

**Artifact paths:**

- `fixtures/oracle.json`
- `fixtures/oracle.md`

**Open issues:**

- None in the classifier.

**Next phase:** P04

## P02 — Create the repository and data contracts

**Phase and status:** P02 PASS

**Files changed:**

- `pom.xml`
- `mvnw`
- `mvnw.cmd`
- `.mvn/wrapper/maven-wrapper.properties`
- `fixtures/research-report.md`
- `fixtures/claims.json`
- `fixtures/MANIFEST.md`
- `fixtures/invalid/`
- `docs/data-contract.md`
- `scripts/build_fixtures.py`
- `src/main/java/numberline/ClaimReader.java`
- `src/main/java/numberline/Spans.java`
- `README.md`

**Commands and results:**

- Toolchain: Java 21.0.13, Maven Wrapper 3.3.4, Maven 3.9.16. Jackson 2.23.0. JUnit 6.1.3.
- `./mvnw -B -ntp verify` accepts `fixtures/claims.json` (24 comparisons, document sha256 `cd335abba1cfbd920e3485b7905b2624f0a5ff6105dd6e22c2f6d0683a510591`).
- The same command rejects `fixtures/invalid/schema.json` (`schema`), `bad-hash.json` (`hash`), `span-split.json` (`span`), `unknown-unit.json` (`unit`), and `nonfinite.json` (`number`).
- `fixtures/MANIFEST.md` checksums match the file bytes.

**Artifact paths:**

- `fixtures/research-report.md`
- `fixtures/claims.json`
- `fixtures/MANIFEST.md`
- `docs/data-contract.md`

**Open issues:**

- None for the contract. Indriya is not a runtime dependency, per the P01 decision.

**Next phase:** P03

## P01 — Research overlap and confirm the MVP

**Phase and status:** P01 PASS

**Files changed:**

- `docs/overlap.md`
- `docs/project-contract.md`
- `docs/grokbot-progress.md`

**Commands and results:**

- Repository inspection: `main` at `ea3bd14`, files `README.md` and `LICENSE` (MIT). Remote `origin` is `Harry0052/numberline`.
- Local toolchain: `openjdk version "21.0.13.0.3"` present. `mvn` was not on `PATH` before the wrapper.
- Indriya 2.2.4 probe, observed: 1.2 kg equivalent to 1200 g in both directions; 8 percent equivalent to 8 percent with no denominator field; USD/EUR and an omitted period are not representable.
- chart-contract 0.2.0 probe, observed on the orders-versus-customers case: verdict `BLOCK`, rule `data.ratio.denominator_basis` FAIL.
- MetricDiff `01567950` probe, observed: text diffs only, no verdict and no normalized magnitude.
- FinQA `0f16e286` evaluator inspection: arithmetic and table operators, no basis classification. The report corpus was not downloaded.

**Artifact paths:**

- `docs/overlap.md`
- `docs/project-contract.md`

**Open issues:**

- chart-contract time-window and currency contracts were read from documentation and not executed. That did not change the proceed decision.

**Next phase:** P02

Decision recorded in `docs/overlap.md`: proceed with a separate local linter.
