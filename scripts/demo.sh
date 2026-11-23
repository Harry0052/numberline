#!/bin/sh
# Build the jar if needed, lint the frozen fixture, and show one invalid input.
set -eu
cd "$(dirname "$0")/.."

./mvnw -B -ntp -DskipTests package

mkdir -p target
java -jar target/numberline-0.1.0.jar lint \
  --document fixtures/research-report.md \
  --claims fixtures/claims.json \
  --format text \
  --output target/demo-report.txt
java -jar target/numberline-0.1.0.jar lint \
  --document fixtures/research-report.md \
  --claims fixtures/claims.json \
  --format json \
  --output target/demo-report.json

rm -f target/invalid-report.txt
set +e
java -jar target/numberline-0.1.0.jar lint \
  --document fixtures/research-report.md \
  --claims fixtures/invalid/nonfinite.json \
  --format text \
  --output target/invalid-report.txt \
  >target/invalid-stdout.txt 2>target/invalid-stderr.txt
status=$?
set -e
if [ "$status" -ne 2 ]; then
  echo "expected invalid input to exit 2, got $status" >&2
  exit 1
fi
if [ -f target/invalid-report.txt ]; then
  echo "invalid input wrote a report" >&2
  exit 1
fi
grep -q "invalid_input: number:" target/invalid-stderr.txt
grep -q "summary: total=24 compatible=10 incompatible=9 unknown=5" target/demo-report.txt
grep -q "C06 verdict=incompatible" target/demo-report.txt
grep -q "NL-DENOMINATOR" target/demo-report.txt

echo "text report: target/demo-report.txt"
echo "json report: target/demo-report.json"
echo "invalid diagnostic: target/invalid-stderr.txt"
