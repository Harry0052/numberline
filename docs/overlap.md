# Overlap: does NumberLine need its own repository?

Access date for every external check: 2026-11-23.

Decision: **proceed**. Build a separate local linter. Do not implement the MVP by contributing the acceptance case to Indriya, FinQA, chart-contract, or MetricDiff.

The target user is a researcher, methods reviewer, or editor who already has a passage and a human-confirmed claim file. The acceptance case is a three-way classification (`compatible`, `incompatible`, or `unknown`) of one binary comparison, using declared basis fields, with normalized magnitudes, exact UTF-8 byte spans, and a reason for every warning. Arithmetic equality and source truth stay separate from that classification.

## Shared fixture

The same four situations were held constant. Expected results are the NumberLine acceptance case, not each tool's native verdict vocabulary.

| Id | Input held constant | Expected classification |
| --- | --- | --- |
| A | 1.2 kg versus 1,200 g, same declared basis | compatible, equal canonical magnitudes, including operand reversal |
| B | 8 percent of orders versus 8 percent of customers, same month, identical numeric strings | incompatible, issue class denominator |
| C | 10 USD versus 10 EUR, same calendar year, identical numeric strings | incompatible, issue class currency |
| D | 8 percent comparison with the reporting period omitted | unknown, because essential basis metadata is missing |

No financial-report corpus was copied. Probe inputs were synthetic.

## Capability matrix

| Capability | Indriya 2.2.4 | FinQA evaluator at `0f16e286` | chart-contract 0.2.0 | MetricDiff `01567950` | NumberLine MVP |
| --- | --- | --- | --- | --- | --- |
| Exact mass conversion, 1.2 kg = 1,200 g, both directions | Yes, observed | No | No physical-unit allowlist. Fixed factors are caller-supplied | No. Values are ignored | Yes, planned |
| Percent scale | Yes, as a dimensionless unit | Strips `%` and divides by 100 inside arithmetic programs | Declared fraction versus percentage-point representation | No | Yes, on an allowlist, after a dimension check |
| Denominator / population kept distinct from the numeric scale | No | No | Yes, for rows of one chart metric | Text field only, across vintages | Yes |
| Reporting-period equality | No | No | Opt-in chart time-window contract | `as_of` dates two snapshots; not a claim window | Yes |
| Currency identity without an exchange rate | No | `$` is stripped before `float` arithmetic | Declared source-unit identity and a fixed factor; no FX fetch in v1 | Units are free text | Yes. Different ISO codes are incompatible |
| Unknown when metadata is missing | No | Invalid programs become `n/a`, which is an execution failure | Missing contracts become REVIEW or BLOCK, which is a chart gate | Missing snapshot fields fail validation | Yes, distinct from invalid input |
| UTF-8 byte spans into a Markdown passage | No | No | No | No | Yes |
| Classification vocabulary | `isCompatible` / `isEquivalentTo` | execution accuracy and symbolic program equality | READY / REVIEW / BLOCK | changed / added / removed | compatible / incompatible / unknown |
| Offline, no paid service, no model | Yes | Evaluator is local; the task corpus is third-party report text | Yes | Yes | Yes |

## Probes

### Indriya — observed run

- Primary URL: https://github.com/unitsofmeasurement/indriya
- Release: 2.2.4 (17 May 2026), commit `4c66f0df7c4c726c7d14e7c64db5c1acae376693`
- Coordinates: `tech.units:indriya:2.2.4`, with `javax.measure:unit-api:2.2` and `tech.uom.lib:uom-lib-common:2.2`
- License: BSD-3-Clause. The repository `LICENSE` text matches that license. GitHub's license API returns `NOASSERTION` for the same file. Maven Central labels the artifact BSD-3-Clause.
- Dataset terms: none. It is a library.
- Jar SHA-256: `803d14c8fd93effad1a22ed8056a19d94a65ddbd9432eea1f3450da23bdea319`
- unit-api 2.2 SHA-256: `667487e1ee57298cdc767885f00b86c899b2fe7e72dc00b6560a6aa64f2bc9c4`
- uom-lib-common 2.2 SHA-256: `a01ece1c236b7b15a431b0383bdddf06dc6d7a85290e9a62b63904e1e4e0dc0d`

Command, from `/tmp`, Java 21.0.13:

```text
javac --class-path indriya-2.2.4.jar:unit-api-2.2.jar:uom-lib-common-2.2.jar:jakarta.inject-api-2.0.1.jar Probe.java
java --class-path ... Probe
```

Observed:

```text
A | 1.2 kg vs 1200 g
  dimension_compatible=true equivalent=true right_in_left_unit=1.2 kg
A-rev | 1200 g vs 1.2 kg
  dimension_compatible=true equivalent=true right_in_left_unit=1200 null
B | 8 percent vs 8 percent
  dimension_compatible=true equivalent=true right_in_left_unit=8 %
C | 10 USD vs 10 EUR
  no currency unit pair and no period field; comparison not representable
D | omitted period
  no unknown state; a bare percent quantity is still constructed
percent_vs_dimensionless_8 compatible=true equivalent=false
```

The gram symbol was null on the reversal conversion. Equivalence was still true, so case A passes inside Indriya. Case B is a false pass for this acceptance case: both sides are the same percent quantity, and the API has no denominator, population, currency, period, or source span. `Units.PERCENT` has dimension `one`. Eight percent is dimension-compatible with the dimensionless value 8 and not equivalent to it, which is scale arithmetic, not basis checking.

Indriya remains the right library if a later version needs a large physical-unit set. The MVP allowlist is four mass units' worth of factors plus rate scales and currency identity. Adding Indriya would not supply the report semantics, and its dimensionless percent type is exactly the collapse the MVP has to refuse. It is not a runtime dependency.

### FinQA — source inspection, benchmark not executed

- Primary URL: https://github.com/czyssrs/FinQA and https://finqasite.github.io/
- Commit inspected: `0f16e2867befa6840783e58be38c9efb9229d742` (2022-06-06)
- Code license: MIT (`https://github.com/czyssrs/FinQA/blob/main/LICENSE`)
- Dataset terms: the repository license does not grant the underlying financial-report excerpts. Those filings have separate rights. This repository does not copy the corpus, and the demo does not download it.

`code/evaluate/evaluate.py` defines the labeled task. `all_ops` is only:

```text
add, subtract, multiply, divide, exp, greater,
table_max, table_min, table_sum, table_average
```

`str_to_num` removes commas, strips `$`, and turns a trailing percent into a float divided by 100. `eval_program` executes those operations. `evaluate_result` scores execution accuracy against `qa.exe_ans` and symbolic program equality. A published record has `pre_text`, `post_text`, `table`, `id`, and `qa` (`question`, `program`, `gold_inds`, `exe_ans`). There is no field for unit dimension, currency identity, reporting window, numerator, denominator, population, UTF-8 span, or a compatible / incompatible / unknown label.

Eligible FinQA cases for this acceptance metric: none. A question can mention years or percents, but the gold label is a number or program, and the evaluator assumes the operands it is told to combine are already comparable. Running the benchmark would not emit the shared-fixture classification, and it would require report text this project must not ship. The check is source and schema inspection, not an unresolved gap.

### chart-contract — observed run, closest discovery hit

- Primary URL: https://github.com/tmusser/chart-contract
- Version: 0.2.0, `pyproject.toml` in commit `80adecbfaeab8d2c6830ce751e6d4430b07379a2` (2026-11-22)
- License: MIT
- Dataset terms: none. Callers supply chart data.
- Why this one: repository search for denominator, period, and currency comparison surfaced its audit rules (`contract.ratio.denominator`, `data.ratio.denominator_basis`, time-window and unit-conversion contracts) ahead of other hits.

Case B was mapped to `Chart.compare` with `denominator_basis_field` set to a column containing `orders` and `customers`. Observed verdict: `BLOCK`. Finding:

```text
data.ratio.denominator_basis FAIL
Denominator basis field 'denominator_id' contains mixed denominator identities: 'customers', 'orders'.
```

That is real evidence that a chart audit can refuse mixed denominators. It does not meet the acceptance case:

- The result is a chart gate (`BLOCK`), not `incompatible` plus a denominator issue class on a binary claim.
- The input is a dataframe and a Vega-Lite-oriented chart, not a Markdown passage and a claim manifest.
- There is no UTF-8 byte span, no excerpt from prose, and no proof that 1.2 kg equals 1,200 g under an allowlist.
- Currency and period checks are separate opt-in chart contracts. They were read in `README.md` and `docs/AUDIT_RULES.md` (documentation). They were not re-run, because the passage-and-span interface does not exist to hold cases A, C, and D constant.
- Documentation also says v1 does not fetch exchange rates and does not silently prorate unequal time windows. That matches a limitation NumberLine will keep, but the product around it is chart sharing.

Contributing the MVP to chart-contract would turn a research-report linter into more chart rules and still would not quote passage spans or score the 24-case fixture. A later export of NumberLine findings into a chart bundle is out of scope.

### MetricDiff — observed run

- Primary URL: https://github.com/josephbsmith/metricdiff
- Commit: `015679505bf6cf29ec6c7010416e5b05b6bff875` (2026-07-17)
- License: MIT
- Script SHA-256 of the file that was executed: `978b1ceba8039af30bf6edc4189c1368c0a5d8ae5e9691d0ff40c9102981aa2e`

Two synthetic snapshots were imported and diffed. Observed changes were text-field edits only: series A `units` `kg` to `g`, series B `denominator` `orders` to `customers`, series C `units` `USD` to `EUR`. The diff did not contain a verdict, a normalized magnitude, or the value `1.2`. The README states that the tool ignores released values and does not decide economic equivalence. There is no span and no unknown state for a missing period; a missing snapshot field is a validation error.

MetricDiff is a methodology-history diff. It is not a comparison linter.

### Other discovery notes

These were documentation checks, not runs. None is closer to the acceptance case than chart-contract.

- `scale-venture-partners/onus` checks quantity strings against evidence. Its own limits say a right number on the wrong subject is outside deterministic tier 1, and part of the pipeline is a model. That fails the offline, no-paid-service, basis-before-arithmetic constraint.
- `warun7/upheld` documents a unit check for length, mass, speed, and time only. Money is matched as digits, so `$410` can pass against `€410`. Denominators, populations, and reporting windows are not the check.
- The parliamentary briefing on statistical spin and the "normalize before truth discovery" note both describe the failure mode (unlike periods, populations, and units). They are guidance, not software that scores the fixture.
- "Language Models Compare Quantities Using Number-specific and Unit-specific Heuristics" (arXiv:2606.03982) studies model heuristics for unit scale. That is the opposite of an exact allowlist with explicit unknown.

Search queries used:

- `GitHub denominator comparison lint statistical claims per capita period currency`
- `GitHub numerical claim validation quantity units time periods fact checking`
- `literature benchmark incomparable quantities denominators numerical claims LLM`

## What already matches

- Indriya already matches case A for ordinary mass units, including reversal.
- chart-contract already matches the denominator half of case B, inside a chart audit.
- FinQA already matches numeric execution of a program over a table, which this project explicitly does not score.
- MetricDiff already matches "show that a denominator string changed," without a magnitude.

None of them matches the whole case: one passage, two declared operands, basis classification independent of arithmetic, normalized values, exact spans, and unknown versus invalid input.

## Remaining uncertainties

- chart-contract's time-window and currency contracts were not executed in this session. The README is specific, and the missing passage/span interface is enough to reject integration. A future comparison could still run those chart contracts on synthetic frames.
- FinQA may contain individual questions whose prose mentions unlike bases. They are not labeled with the acceptance classes, and the corpus stays out of this repository.
- Indriya 2.2.4 was probed as a library call, not through its full test suite.
- No package-registry tool under the search queries advertised a Markdown claim linter with this three-way metric.

## Decision

Proceed with a new repository and a small Java 21 core. The fixture provenance path is original prose under the repository MIT license, with no remote data at run time. Dependencies for the build are an Apache Maven Wrapper distribution, Jackson under Apache-2.0, and JUnit under EPL-2.0 at test scope. Indriya is documented as the unit-system reference and is not linked into the runtime. Integration or contribution is not recommended for the MVP, because the closest working workflow (chart-contract) covers only a chart-scoped part of one case and does not satisfy the frozen classification metric.
