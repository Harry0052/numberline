# Grokbot progress

## P01 — Research overlap and confirm the MVP

**Phase and status:** P01 PASS

**Files changed:**

- `docs/overlap.md`
- `docs/project-contract.md`
- `docs/grokbot-progress.md`

**Commands and results:**

- Repository inspection: `main` at `ea3bd14`, files `README.md` and `LICENSE` (MIT, copyright 2026 Harry Malhi). Remote `origin` is `Harry0052/numberline`.
- Local toolchain: `openjdk version "21.0.13.0.3"` present. `mvn` was not on `PATH` (wrapper is introduced in P02).
- Indriya 2.2.4 probe, observed: 1.2 kg equivalent to 1200 g in both directions; 8 percent equivalent to 8 percent with no denominator field; USD/EUR and omitted period are not representable. 8 percent is dimension-compatible with dimensionless 8 and not equivalent to it.
- chart-contract 0.2.0 probe, observed on the orders-versus-customers case: verdict `BLOCK`, rule `data.ratio.denominator_basis` FAIL for mixed `orders` and `customers`.
- MetricDiff `01567950` probe, observed: text diffs for `kg`/`g`, `orders`/`customers`, and `USD`/`EUR`, with no verdict and no normalized magnitude.
- FinQA `0f16e286` `code/evaluate/evaluate.py` inspection: operators are arithmetic and table aggregation; labels are execution results, not basis classes. The report corpus was not downloaded.

**Artifact paths:**

- `docs/overlap.md`
- `docs/project-contract.md`
- Probe sources were temporary (`/tmp/nl-probe`, `/tmp/metricdiff`, `/tmp/chart-contract`) and are not part of the repository.

**Open issues:**

- chart-contract time-window and currency contracts were read from documentation and not executed. That does not change the proceed decision.
- Maven Wrapper is not set up yet.

**Next phase:** P02

Decision recorded in `docs/overlap.md`: proceed with a separate local linter. The acceptance case and dependency list are in `docs/project-contract.md`.
