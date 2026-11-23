#!/usr/bin/env python3
"""Write the frozen NumberLine fixtures from original prose.

The script is deterministic. There is no random seed and no network call.
Expected magnitudes are literals checked with decimal.Decimal, which is
independent of the Java engine.
"""

from __future__ import annotations

import hashlib
import json
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "fixtures"

YEAR_2019 = ("2019-01-01", "2019-12-31")
YEAR_2020 = ("2020-01-01", "2020-12-31")
YEAR_2024 = ("2024-01-01", "2024-12-31")
MARCH = ("2024-03-01", "2024-03-31")
HALF_2024 = ("2024-01-01", "2024-06-30")


def plain(value: Decimal) -> str:
    text = format(value, "f")
    if "." in text:
        text = text.rstrip("0").rstrip(".")
    return text


def mass_g(value: str, unit: str) -> Decimal:
    number = Decimal(value.replace(",", ""))
    factor = {"mg": Decimal("0.001"), "g": Decimal(1), "kg": Decimal(1000)}[unit]
    return number * factor


def rate(value: str, unit: str) -> Decimal:
    number = Decimal(value.replace(",", ""))
    factor = {
        "percent": Decimal("0.01"),
        "per_100": Decimal("0.01"),
        "per_1000": Decimal("0.001"),
        "proportion": Decimal(1),
    }[unit]
    return number * factor


def money(value: str) -> Decimal:
    return Decimal(value.replace(",", ""))


assert plain(mass_g("1.2", "kg")) == "1200"
assert plain(mass_g("1,200", "g")) == "1200"
assert plain(mass_g("1,200,000", "mg")) == "1200"
assert plain(mass_g("1.2", "g")) == "1.2"
assert plain(mass_g("2", "kg")) == "2000"
assert plain(mass_g("2,000", "g")) == "2000"
assert plain(rate("8", "percent")) == "0.08"
assert plain(rate("80", "per_1000")) == "0.08"
assert plain(rate("0.08", "proportion")) == "0.08"
assert plain(money("10")) == "10"


def operand(phrase, value, unit, currency, period, numerator, denominator, population):
    return {
        "phrase": phrase,
        "value": value,
        "unit": unit,
        "currency": currency,
        "periodStart": None if period is None else period[0],
        "periodEnd": None if period is None else period[1],
        "numerator": numerator,
        "denominator": denominator,
        "populationKey": population,
    }


def mass(phrase, value, unit, period=YEAR_2024, population="cohort-alpha"):
    return operand(phrase, value, unit, None, period, None, None, population)


def rate_op(phrase, value, unit, numerator, denominator, population, period=YEAR_2024):
    return operand(phrase, value, unit, None, period, numerator, denominator, population)


def money_op(phrase, value, currency, period, population="program-budget"):
    return operand(phrase, value, "money", currency, period, None, None, population)


CASES = [
    {
        "id": "C01",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "1200",
        "rightCanonical": "1200",
        "arithmetic": "equal",
        "handCalc": "1.2 kg = 1.2 × 1000 g = 1200 g. 1,200 g = 1200 g. Period, population, and currency match, so the basis is compatible and the magnitudes are equal.",
        "left": mass("Alpha specimen mass 1.2 kg", "1.2", "kg"),
        "right": mass("Alpha specimen mass 1,200 g", "1,200", "g"),
    },
    {
        "id": "C02",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "1200",
        "rightCanonical": "1200",
        "arithmetic": "equal",
        "handCalc": "Operand reversal of C01. 1,200 g = 1200 g and 1.2 kg = 1200 g. Equality does not depend on which side is written first.",
        "left": mass("Reversed specimen mass 1,200 g", "1,200", "g"),
        "right": mass("Reversed specimen mass 1.2 kg", "1.2", "kg"),
    },
    {
        "id": "C03",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "1200",
        "rightCanonical": "1200",
        "arithmetic": "equal",
        "handCalc": "1.2 kg = 1200 g. 1,200,000 mg = 1,200,000 / 1000 g = 1200 g.",
        "left": mass("Aliquot reference mass 1.2 kg", "1.2", "kg"),
        "right": mass("Aliquot reference mass 1,200,000 mg", "1,200,000", "mg"),
    },
    {
        "id": "C04",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "1200",
        "rightCanonical": "1.2",
        "arithmetic": "mismatch",
        "handCalc": "1.2 kg = 1200 g and 1.2 g = 1.2 g. The basis matches, so the comparison is allowed, and the magnitudes are not equal. Compatibility is not arithmetic equality.",
        "left": mass("Mismatch specimen mass 1.2 kg", "1.2", "kg"),
        "right": mass("Mismatch specimen mass 1.2 g", "1.2", "g"),
    },
    {
        "id": "C05",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_unit"],
        "basisFields": ["unit"],
        "conversionAttempted": False,
        "conversionApplied": False,
        "canonicalUnit": None,
        "normalizedStatus": "not_evaluated",
        "leftCanonical": None,
        "rightCanonical": None,
        "arithmetic": "not_evaluated",
        "handCalc": "1.2 kg is mass and 1.2 count is a count. The numeric strings match. Dimensions differ, so no conversion is attempted.",
        "left": mass("Dimension specimen mass 1.2 kg", "1.2", "kg"),
        "right": mass("Dimension specimen tally 1.2 count", "1.2", "count"),
    },
    {
        "id": "C06",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_denominator"],
        "basisFields": ["denominator"],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both sides are 8 percent = 0.08. Denominators are orders placed versus customers billed. Equal scales do not make the bases comparable.",
        "left": rate_op(
            "March completed share 8 percent",
            "8",
            "percent",
            "selected events",
            "orders placed",
            "march-activity",
            MARCH,
        ),
        "right": rate_op(
            "March customer share 8 percent",
            "8",
            "percent",
            "selected events",
            "customers billed",
            "march-activity",
            MARCH,
        ),
    },
    {
        "id": "C07",
        "verdict": "unknown",
        "issueClasses": ["unknown_missing_period"],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both sides are 8 percent of customers billed and the population matches, but neither side declares a period. The classification is unknown rather than compatible.",
        "left": rate_op(
            "Undated left share 8 percent",
            "8",
            "percent",
            "selected events",
            "customers billed",
            "march-activity",
            None,
        ),
        "right": rate_op(
            "Undated right share 8 percent",
            "8",
            "percent",
            "selected events",
            "customers billed",
            "march-activity",
            None,
        ),
    },
    {
        "id": "C08",
        "verdict": "unknown",
        "issueClasses": ["unknown_missing_population"],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "1200",
        "rightCanonical": "1200",
        "arithmetic": "not_evaluated",
        "handCalc": "1.2 kg = 1200 g and 1,200 g = 1200 g, but the population key is omitted on both sides. Equal mass does not fill in a missing population.",
        "left": mass("Unassigned specimen mass 1.2 kg", "1.2", "kg", population=None),
        "right": mass("Unassigned specimen mass 1,200 g", "1,200", "g", population=None),
    },
    {
        "id": "C09",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_period"],
        "basisFields": ["period"],
        "conversionAttempted": True,
        "conversionApplied": False,
        "canonicalUnit": "USD",
        "normalizedStatus": "shown",
        "leftCanonical": "10",
        "rightCanonical": "10",
        "arithmetic": "not_evaluated",
        "handCalc": "Both amounts are 10 USD. The windows are calendar 2019 and calendar 2020. No inflation factor is applied, so the period mismatch stands on its own.",
        "left": money_op("Year 2019 program cost 10 USD", "10", "USD", YEAR_2019),
        "right": money_op("Year 2020 program cost 10 USD", "10", "USD", YEAR_2020),
    },
    {
        "id": "C10",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_currency"],
        "basisFields": ["currency"],
        "conversionAttempted": False,
        "conversionApplied": False,
        "canonicalUnit": None,
        "normalizedStatus": "not_evaluated",
        "leftCanonical": None,
        "rightCanonical": None,
        "arithmetic": "not_evaluated",
        "handCalc": "10 USD and 10 EUR share a year and a numeric string. No exchange rate is applied, so the currency mismatch is incompatible and magnitudes are not placed on one scale.",
        "left": money_op("Currency left program cost 10 USD", "10", "USD", YEAR_2020),
        "right": money_op("Currency right program cost 10 EUR", "10", "EUR", YEAR_2020),
    },
    {
        "id": "C11",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": False,
        "canonicalUnit": "USD",
        "normalizedStatus": "shown",
        "leftCanonical": "10",
        "rightCanonical": "10",
        "arithmetic": "equal",
        "handCalc": "Both sides are 10 USD for calendar 2020 and the same program population. No scale factor is required. The right-hand excerpt deliberately contains markup.",
        "left": money_op("Matched plain program cost 10 USD", "10", "USD", YEAR_2020),
        "right": money_op(
            "Matched marked program cost 10 USD <script>alert(1)</script> **bold**",
            "10",
            "USD",
            YEAR_2020,
        ),
    },
    {
        "id": "C12",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "equal",
        "handCalc": "8 percent = 8/100 = 0.08. 80 per 1,000 = 80/1000 = 0.08. Numerator, denominator, population, and period match.",
        "left": rate_op(
            "Normalized event share 8 percent",
            "8",
            "percent",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Normalized event share 80 per_1000",
            "80",
            "per_1000",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
    },
    {
        "id": "C13",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "equal",
        "handCalc": "Operand reversal of the C12 rate normalization. 80/1000 = 0.08 and 8/100 = 0.08.",
        "left": rate_op(
            "Reversed event share 80 per_1000",
            "80",
            "per_1000",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Reversed event share 8 percent",
            "8",
            "percent",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
    },
    {
        "id": "C14",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "equal",
        "handCalc": "0.08 proportion = 0.08. 8 percent = 0.08. Same event, patient, and cohort basis.",
        "left": rate_op(
            "Proportion event share 0.08 proportion",
            "0.08",
            "proportion",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Proportion event share 8 percent",
            "8",
            "percent",
            "events",
            "patients enrolled",
            "cohort-alpha",
        ),
    },
    {
        "id": "C15",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_population"],
        "basisFields": ["population"],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both sides say 8 percent of patients enrolled. Population keys are cohort-alpha and cohort-beta, so the populations are not the same.",
        "left": rate_op(
            "Alpha mortality share 8 percent",
            "8",
            "percent",
            "mortality",
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Beta mortality share 8 percent",
            "8",
            "percent",
            "mortality",
            "patients enrolled",
            "cohort-beta",
        ),
    },
    {
        "id": "C16",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_denominator"],
        "basisFields": ["denominator"],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "8 percent of patients and 8 percent of hospitals both scale to 0.08. Per-patient and per-hospital percentages stay incompatible.",
        "left": rate_op(
            "Patient occupancy share 8 percent",
            "8",
            "percent",
            "occupied status",
            "patients",
            "health-system",
        ),
        "right": rate_op(
            "Hospital occupancy share 8 percent",
            "8",
            "percent",
            "occupied status",
            "hospitals",
            "health-system",
        ),
    },
    {
        "id": "C17",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_numerator"],
        "basisFields": ["numerator"],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both are 8 percent of patients enrolled in cohort Alpha. Numerators are deaths versus admissions.",
        "left": rate_op(
            "Death numerator share 8 percent",
            "8",
            "percent",
            "deaths",
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Admission numerator share 8 percent",
            "8",
            "percent",
            "admissions",
            "patients enrolled",
            "cohort-alpha",
        ),
    },
    {
        "id": "C18",
        "verdict": "unknown",
        "issueClasses": ["unknown_missing_denominator"],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both values are 8 percent, but the denominator definition is null. A percent without a denominator is unknown, not a comparable proportion.",
        "left": rate_op(
            "Missing denominator left share 8 percent",
            "8",
            "percent",
            "selected events",
            None,
            "cohort-alpha",
        ),
        "right": rate_op(
            "Missing denominator right share 8 percent",
            "8",
            "percent",
            "selected events",
            None,
            "cohort-alpha",
        ),
    },
    {
        "id": "C19",
        "verdict": "unknown",
        "issueClasses": ["unknown_missing_currency"],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": False,
        "canonicalUnit": "money",
        "normalizedStatus": "shown",
        "leftCanonical": "10",
        "rightCanonical": "10",
        "arithmetic": "not_evaluated",
        "handCalc": "Both amounts are 10 money units with the currency code omitted. They are not treated as the same currency, and they are not converted.",
        "left": money_op("Unspecified currency cost 10 money", "10", None, YEAR_2020),
        "right": money_op("Unspecified currency twin 10 money", "10", None, YEAR_2020),
    },
    {
        "id": "C20",
        "verdict": "unknown",
        "issueClasses": ["unknown_missing_numerator"],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "not_evaluated",
        "handCalc": "Both sides are 8 percent of patients enrolled, but the numerator definition is null. The rate basis is incomplete.",
        "left": rate_op(
            "Missing numerator left share 8 percent",
            "8",
            "percent",
            None,
            "patients enrolled",
            "cohort-alpha",
        ),
        "right": rate_op(
            "Missing numerator right share 8 percent",
            "8",
            "percent",
            None,
            "patients enrolled",
            "cohort-alpha",
        ),
    },
    {
        "id": "C21",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_currency", "incompatible_period"],
        "basisFields": ["currency", "period"],
        "conversionAttempted": False,
        "conversionApplied": False,
        "canonicalUnit": None,
        "normalizedStatus": "not_evaluated",
        "leftCanonical": None,
        "rightCanonical": None,
        "arithmetic": "not_evaluated",
        "handCalc": "10 USD in 2019 versus 10 EUR in 2020. The numeric strings match. Currency and period are flagged independently, and no exchange or inflation factor is applied.",
        "left": money_op("Cross basis cost 10 USD", "10", "USD", YEAR_2019),
        "right": money_op("Cross basis cost 10 EUR", "10", "EUR", YEAR_2020),
    },
    {
        "id": "C22",
        "verdict": "incompatible",
        "issueClasses": ["incompatible_period"],
        "basisFields": ["period"],
        "conversionAttempted": True,
        "conversionApplied": False,
        "canonicalUnit": "USD",
        "normalizedStatus": "shown",
        "leftCanonical": "10",
        "rightCanonical": "10",
        "arithmetic": "not_evaluated",
        "handCalc": "Both amounts are 10 USD for the same program. The windows are 1 January through 30 June 2024 and 1 January through 31 December 2024. The shorter window is not prorated.",
        "left": money_op("Half year program cost 10 USD", "10", "USD", HALF_2024),
        "right": money_op("Full year program cost 10 USD", "10", "USD", YEAR_2024),
    },
    {
        "id": "C23",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "proportion",
        "normalizedStatus": "shown",
        "leftCanonical": "0.08",
        "rightCanonical": "0.08",
        "arithmetic": "equal",
        "handCalc": "Both sides are 8 percent of customers billed for March 2024 with the same numerator and population. 8/100 = 0.08 on each side.",
        "left": rate_op(
            "Aligned customer share 8 percent",
            "8",
            "percent",
            "selected events",
            "customers billed",
            "march-activity",
            MARCH,
        ),
        "right": rate_op(
            "Aligned customer match 8 percent",
            "8",
            "percent",
            "selected events",
            "customers billed",
            "march-activity",
            MARCH,
        ),
    },
    {
        "id": "C24",
        "verdict": "compatible",
        "issueClasses": [],
        "basisFields": [],
        "conversionAttempted": True,
        "conversionApplied": True,
        "canonicalUnit": "g",
        "normalizedStatus": "shown",
        "leftCanonical": "2000",
        "rightCanonical": "2000",
        "arithmetic": "equal",
        "handCalc": "2 kg = 2000 g. 2,000 g = 2000 g. The surrounding passage contains multibyte characters; these spans stay on code-point boundaries.",
        "left": mass("Crate specimen mass 2 kg", "2", "kg"),
        "right": mass("Crate specimen mass 2,000 g", "2,000", "g"),
    },
]

assert len(CASES) == 24
assert len({case["id"] for case in CASES}) == 24

INTRO = """# Synthetic methods note

This passage is original fixture text written for NumberLine. It is not a clinical record and it is not copied from a published financial report. A naïve label, the clinic word 患者, and the symbol µg sit in this introduction so later span checks have multibyte characters to measure. Reviewers confirm every structured field in the claim file before a run.

## Recorded figures

"""

phrases = [side["phrase"] for case in CASES for side in (case["left"], case["right"])]
for left in phrases:
    for right in phrases:
        if left != right and left in right:
            raise SystemExit(f"phrase is a substring of another: {left!r} in {right!r}")

body = "".join(side["phrase"] + ".\n" for case in CASES for side in (case["left"], case["right"]))
document = INTRO + body
raw = document.encode("utf-8")
for phrase in phrases:
    encoded = phrase.encode("utf-8")
    if raw.count(encoded) != 1:
        raise SystemExit(f"phrase is not unique in the passage: {phrase}")
    if "µg" not in document or "患者" not in document or "naïve" not in document:
        raise SystemExit("multibyte markers missing")

digest = hashlib.sha256(raw).hexdigest()


def side_json(case_id, suffix, side, start, end):
    return {
        "sourceId": case_id + suffix,
        "span": {"start": start, "end": end},
        "value": side["value"],
        "unit": side["unit"],
        "currency": side["currency"],
        "periodStart": side["periodStart"],
        "periodEnd": side["periodEnd"],
        "numerator": side["numerator"],
        "denominator": side["denominator"],
        "populationKey": side["populationKey"],
    }


comparisons = []
oracle_cases = []
for case in CASES:
    left_phrase = case["left"]["phrase"].encode("utf-8")
    right_phrase = case["right"]["phrase"].encode("utf-8")
    left_start = raw.find(left_phrase)
    right_start = raw.find(right_phrase)
    comparisons.append(
        {
            "id": case["id"],
            "operation": "compare",
            "left": side_json(case["id"], "L", case["left"], left_start, left_start + len(left_phrase)),
            "right": side_json(case["id"], "R", case["right"], right_start, right_start + len(right_phrase)),
        }
    )
    oracle_cases.append(
        {
            "id": case["id"],
            "verdict": case["verdict"],
            "issueClasses": case["issueClasses"],
            "basisFields": case["basisFields"],
            "missingFields": [
                name
                for name, issue in (
                    ("period", "unknown_missing_period"),
                    ("population", "unknown_missing_population"),
                    ("currency", "unknown_missing_currency"),
                    ("numerator", "unknown_missing_numerator"),
                    ("denominator", "unknown_missing_denominator"),
                )
                if issue in case["issueClasses"]
            ],
            "conversionAttempted": case["conversionAttempted"],
            "conversionApplied": case["conversionApplied"],
            "canonicalUnit": case["canonicalUnit"],
            "normalizedStatus": case["normalizedStatus"],
            "leftCanonical": case["leftCanonical"],
            "rightCanonical": case["rightCanonical"],
            "arithmetic": case["arithmetic"],
            "leftExcerpt": case["left"]["phrase"],
            "rightExcerpt": case["right"]["phrase"],
            "handCalc": case["handCalc"],
        }
    )

claims = {
    "schemaVersion": "1",
    "document": {
        "id": "synthetic-methods-note",
        "sha256": digest,
        "encoding": "UTF-8",
    },
    "comparisons": comparisons,
}
oracle = {
    "schemaVersion": "1",
    "metric": "24-case basis classification",
    "note": "Hand calculations use exact decimal factors. They were checked with Python decimal.Decimal and are not produced by the Java engine.",
    "cases": oracle_cases,
}


def dump(path: Path, payload) -> None:
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


(FIXTURES / "research-report.md").write_bytes(raw)
dump(FIXTURES / "claims.json", claims)
dump(FIXTURES / "oracle.json", oracle)

counts = {"compatible": 0, "incompatible": 0, "unknown": 0}
issue_counts: dict[str, int] = {}
for case in oracle_cases:
    counts[case["verdict"]] += 1
    for issue in case["issueClasses"]:
        issue_counts[issue] = issue_counts.get(issue, 0) + 1

lines = [
    "# Oracle for the frozen 24-case fixture",
    "",
    "These calculations are independent of the Java engine. Factors are exact:",
    "",
    "- 1 kg = 1000 g, 1 g = 1000 mg",
    "- 1 percent = 1/100, 1 per_1000 = 1/1000, proportion is already a share of one",
    "- money amounts are not converted across currencies or years",
    "",
    "A row is `compatible` only when every required basis field is present and equal and the units share a dimension. Magnitudes are compared only after that. `unknown` means a required field is missing. `incompatible` means the declared fields conflict. Invalid input is not a row in this table.",
    "",
    f"Counts: compatible={counts['compatible']}, incompatible={counts['incompatible']}, unknown={counts['unknown']}.",
    "",
    "Issue-class hits, counting both classes on a row that has two:",
    "",
]
for name in sorted(issue_counts):
    lines.append(f"- {name}: {issue_counts[name]}")
lines.append("")
for case in oracle_cases:
    lines.append(f"## {case['id']} — {case['verdict']}")
    lines.append("")
    lines.append(case["handCalc"])
    lines.append("")
    lines.append(
        f"Issue classes: {', '.join(case['issueClasses']) if case['issueClasses'] else 'none'}."
    )
    if case["normalizedStatus"] == "shown":
        lines.append(
            f"Canonical {case['canonicalUnit']}: left {case['leftCanonical']}, right {case['rightCanonical']}."
        )
    else:
        lines.append("Canonical magnitudes are not evaluated.")
    lines.append(f"Arithmetic: {case['arithmetic']}.")
    lines.append("")
(FIXTURES / "oracle.md").write_text("\n".join(lines), encoding="utf-8")


def one_comparison(mutation):
    payload = json.loads(json.dumps(claims))
    payload["comparisons"] = [json.loads(json.dumps(payload["comparisons"][0]))]
    mutation(payload)
    return payload


micro = document.encode("utf-8").find("µg".encode("utf-8"))
if micro < 0 or document.encode("utf-8")[micro : micro + 2] != bytes([0xC2, 0xB5]):
    raise SystemExit("micro sign was not the expected UTF-8 sequence")


def span_split(payload):
    payload["comparisons"][0]["left"]["span"] = {"start": micro + 1, "end": micro + 2}


def unknown_unit(payload):
    payload["comparisons"][0]["left"]["unit"] = "furlong"


def nonfinite(payload):
    payload["comparisons"][0]["left"]["value"] = "NaN"


def bad_hash(payload):
    payload["document"]["sha256"] = "0" * 64


def extra_property(payload):
    payload["note"] = "not part of the schema"


invalid_dir = FIXTURES / "invalid"
dump(invalid_dir / "span-split.json", one_comparison(span_split))
dump(invalid_dir / "unknown-unit.json", one_comparison(unknown_unit))
dump(invalid_dir / "nonfinite.json", one_comparison(nonfinite))
dump(invalid_dir / "bad-hash.json", one_comparison(bad_hash))
dump(invalid_dir / "schema.json", one_comparison(extra_property))

hashed = [
    FIXTURES / "research-report.md",
    FIXTURES / "claims.json",
    FIXTURES / "oracle.json",
    FIXTURES / "oracle.md",
    invalid_dir / "span-split.json",
    invalid_dir / "unknown-unit.json",
    invalid_dir / "nonfinite.json",
    invalid_dir / "bad-hash.json",
    invalid_dir / "schema.json",
]
manifest = [
    "# Fixture manifest",
    "",
    "The passage and claims are original text written for this repository.",
    "They are covered by the repository MIT license. No external benchmark",
    "or financial report was copied. Generation has no random seed.",
    "",
    "Regenerate with `python3 scripts/build_fixtures.py`. The script does not",
    "fetch data. Checksums below are SHA-256 of the file bytes.",
    "",
    f"- Generator: scripts/build_fixtures.py",
    f"- Passage SHA-256: {digest}",
    f"- Passage bytes: {len(raw)}",
    f"- Comparisons: 24",
    "",
]
for path in hashed:
    file_digest = hashlib.sha256(path.read_bytes()).hexdigest()
    manifest.append(f"- `{path.relative_to(ROOT).as_posix()}` sha256 `{file_digest}`")
manifest.append("")
(FIXTURES / "MANIFEST.md").write_text("\n".join(manifest), encoding="utf-8")
print(f"wrote fixtures sha256={digest} bytes={len(raw)} counts={counts} issues={issue_counts}")
