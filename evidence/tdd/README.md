# TDD Red-Green-Refactor: UT-14 (AdminSetupRunner.isLoopbackPostgres, IPv6 `[::1]`)

Requirement: admin setup must accept only a loopback PostgreSQL database (FR-06.9). Suspected bug (analysis report 2.6): `URI.getHost()` returns `[::1]` with brackets, but the code only compared `::1`.

Date: 2026-11-24. Java 25.0.5, Maven 3.9.16 (wrapper). Commands: `.\mvnw.cmd -B -o test -Dtest=AdminSetupRunnerLoopbackTest` (full suite for step 4).

| Step | What was done | Actual result | Evidence |
|---|---|---|---|
| Red 1 | Wrote `AdminSetupRunnerLoopbackTest` (6 tests) before touching application code | Compilation error: `isLoopbackPostgres` has private access | `01-red-compile-error.txt` |
| Red 2 | Seam only: removed `private` from `isLoopbackPostgres` (no behaviour change) | Tests run: 6, Failures: 1. `ipv6LoopbackInBracketsIsLoopback`: expected true but was false. Bug confirmed | `02-red-assertion-failure.txt` |
| Green | Added `host.equals("[::1]")` to the host check | Tests run: 6, Failures: 0. BUILD SUCCESS | `03-green.txt` |
| Refactor | Extracted `isLoopbackHost(String)` helper with a comment; behaviour unchanged | Full suite: Tests run: 78, Failures: 0, Errors: 0. BUILD SUCCESS (72 baseline + 6 new) | `04-refactor-full-suite.txt` |

Application files changed (both in `project/backend`):
- `src/main/java/lk/publiccare/backend/admin/AdminSetupRunner.java`: visibility `private` -> package-private; `[::1]` accepted; helper `isLoopbackHost` extracted.
- New test: `src/test/java/lk/publiccare/backend/admin/AdminSetupRunnerLoopbackTest.java`.

Limitation: runs were on Java 25, not the project's Java 21. The change is in the isolated QA copy only; per the test plan (7.11) the project owner would need to approve it for the original repository.
