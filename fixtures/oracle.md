# Oracle for the frozen 24-case fixture

These calculations are independent of the Java engine. Factors are exact:

- 1 kg = 1000 g, 1 g = 1000 mg
- 1 percent = 1/100, 1 per_1000 = 1/1000, proportion is already a share of one
- money amounts are not converted across currencies or years

A row is `compatible` only when every required basis field is present and equal and the units share a dimension. Magnitudes are compared only after that. `unknown` means a required field is missing. `incompatible` means the declared fields conflict. Invalid input is not a row in this table.

Counts: compatible=10, incompatible=9, unknown=5.

Issue-class hits, counting both classes on a row that has two:

- incompatible_currency: 2
- incompatible_denominator: 2
- incompatible_numerator: 1
- incompatible_period: 3
- incompatible_population: 1
- incompatible_unit: 1
- unknown_missing_currency: 1
- unknown_missing_denominator: 1
- unknown_missing_numerator: 1
- unknown_missing_period: 1
- unknown_missing_population: 1

## C01 — compatible

1.2 kg = 1.2 × 1000 g = 1200 g. 1,200 g = 1200 g. Period, population, and currency match, so the basis is compatible and the magnitudes are equal.

Issue classes: none.
Canonical g: left 1200, right 1200.
Arithmetic: equal.

## C02 — compatible

Operand reversal of C01. 1,200 g = 1200 g and 1.2 kg = 1200 g. Equality does not depend on which side is written first.

Issue classes: none.
Canonical g: left 1200, right 1200.
Arithmetic: equal.

## C03 — compatible

1.2 kg = 1200 g. 1,200,000 mg = 1,200,000 / 1000 g = 1200 g.

Issue classes: none.
Canonical g: left 1200, right 1200.
Arithmetic: equal.

## C04 — compatible

1.2 kg = 1200 g and 1.2 g = 1.2 g. The basis matches, so the comparison is allowed, and the magnitudes are not equal. Compatibility is not arithmetic equality.

Issue classes: none.
Canonical g: left 1200, right 1.2.
Arithmetic: mismatch.

## C05 — incompatible

1.2 kg is mass and 1.2 count is a count. The numeric strings match. Dimensions differ, so no conversion is attempted.

Issue classes: incompatible_unit.
Canonical magnitudes are not evaluated.
Arithmetic: not_evaluated.

## C06 — incompatible

Both sides are 8 percent = 0.08. Denominators are orders placed versus customers billed. Equal scales do not make the bases comparable.

Issue classes: incompatible_denominator.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C07 — unknown

Both sides are 8 percent of customers billed and the population matches, but neither side declares a period. The classification is unknown rather than compatible.

Issue classes: unknown_missing_period.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C08 — unknown

1.2 kg = 1200 g and 1,200 g = 1200 g, but the population key is omitted on both sides. Equal mass does not fill in a missing population.

Issue classes: unknown_missing_population.
Canonical g: left 1200, right 1200.
Arithmetic: not_evaluated.

## C09 — incompatible

Both amounts are 10 USD. The windows are calendar 2019 and calendar 2020. No inflation factor is applied, so the period mismatch stands on its own.

Issue classes: incompatible_period.
Canonical USD: left 10, right 10.
Arithmetic: not_evaluated.

## C10 — incompatible

10 USD and 10 EUR share a year and a numeric string. No exchange rate is applied, so the currency mismatch is incompatible and magnitudes are not placed on one scale.

Issue classes: incompatible_currency.
Canonical magnitudes are not evaluated.
Arithmetic: not_evaluated.

## C11 — compatible

Both sides are 10 USD for calendar 2020 and the same program population. No scale factor is required. The right-hand excerpt deliberately contains markup.

Issue classes: none.
Canonical USD: left 10, right 10.
Arithmetic: equal.

## C12 — compatible

8 percent = 8/100 = 0.08. 80 per 1,000 = 80/1000 = 0.08. Numerator, denominator, population, and period match.

Issue classes: none.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: equal.

## C13 — compatible

Operand reversal of the C12 rate normalization. 80/1000 = 0.08 and 8/100 = 0.08.

Issue classes: none.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: equal.

## C14 — compatible

0.08 proportion = 0.08. 8 percent = 0.08. Same event, patient, and cohort basis.

Issue classes: none.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: equal.

## C15 — incompatible

Both sides say 8 percent of patients enrolled. Population keys are cohort-alpha and cohort-beta, so the populations are not the same.

Issue classes: incompatible_population.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C16 — incompatible

8 percent of patients and 8 percent of hospitals both scale to 0.08. Per-patient and per-hospital percentages stay incompatible.

Issue classes: incompatible_denominator.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C17 — incompatible

Both are 8 percent of patients enrolled in cohort Alpha. Numerators are deaths versus admissions.

Issue classes: incompatible_numerator.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C18 — unknown

Both values are 8 percent, but the denominator definition is null. A percent without a denominator is unknown, not a comparable proportion.

Issue classes: unknown_missing_denominator.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C19 — unknown

Both amounts are 10 money units with the currency code omitted. They are not treated as the same currency, and they are not converted.

Issue classes: unknown_missing_currency.
Canonical money: left 10, right 10.
Arithmetic: not_evaluated.

## C20 — unknown

Both sides are 8 percent of patients enrolled, but the numerator definition is null. The rate basis is incomplete.

Issue classes: unknown_missing_numerator.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: not_evaluated.

## C21 — incompatible

10 USD in 2019 versus 10 EUR in 2020. The numeric strings match. Currency and period are flagged independently, and no exchange or inflation factor is applied.

Issue classes: incompatible_currency, incompatible_period.
Canonical magnitudes are not evaluated.
Arithmetic: not_evaluated.

## C22 — incompatible

Both amounts are 10 USD for the same program. The windows are 1 January through 30 June 2024 and 1 January through 31 December 2024. The shorter window is not prorated.

Issue classes: incompatible_period.
Canonical USD: left 10, right 10.
Arithmetic: not_evaluated.

## C23 — compatible

Both sides are 8 percent of customers billed for March 2024 with the same numerator and population. 8/100 = 0.08 on each side.

Issue classes: none.
Canonical proportion: left 0.08, right 0.08.
Arithmetic: equal.

## C24 — compatible

2 kg = 2000 g. 2,000 g = 2000 g. The surrounding passage contains multibyte characters; these spans stay on code-point boundaries.

Issue classes: none.
Canonical g: left 2000, right 2000.
Arithmetic: equal.
