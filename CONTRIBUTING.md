# Contributing

NumberLine compares declared basis fields before it compares magnitudes. Changes should keep that order visible in the report and in the tests.

## Setup

Install Java 21, then run:

```sh
./mvnw -B -ntp verify
```

The fixture demo is `./scripts/demo.sh`.

## Fixtures

Passages and claims in `fixtures/` are original text under the repository MIT license. Do not add financial filings, clinical records, or other third-party corpora. If you change `fixtures/research-report.md`, regenerate the claims and checksums with:

```sh
python3 scripts/build_fixtures.py
```

The script does not download data. Update `fixtures/oracle.md` when a hand calculation changes, and keep the 24-case metric green.

## Commits

Use a conventional commit prefix (`feat`, `fix`, `docs`, `test`, `chore`, `refactor`). Keep the change focused.

## What the tests lock

`FixtureTest` checks every frozen row against `fixtures/oracle.json`: verdict, issue classes, normalized magnitudes, conversion flags, rule ids, and excerpts. Invalid files must return `invalid_input` rather than a classification. Report tests check that markup in an excerpt is escaped in the text report and in the JSON bytes.
