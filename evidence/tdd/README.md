# TDD Red-Green-Refactor: UT-14 (AdminSetupRunner.isLoopbackPostgres, IPv6 `[::1]`)

Requirement: admin setup must accept only a loopback PostgreSQL database (FR-06.9). Suspected bug (analysis report 2.6): `URI.getHost()` returns `[::1]` with brackets, but the code only compared `::1`.

Date: 2026-11-24. Java 25.0.5, Maven 3.9.16 (wrapper). Commands: `.\mvnw.cmd -B -o test -Dtest=AdminSetupRunnerLoopbackTest` (full suite for step 4).

| Step | What was done | Actual result | Evidence |
|---|---|---|---|
| Red 1 | Wrote `AdminSetupRunnerLoopbackTest` (6 tests) before touching application code | Compilation error: `isLoopbackPostgres` has private access | `01-red-compile-error.txt` |
| Red 2 | Seam only: removed `private` from `isLoopbackPostgres` (no behaviour change) | Tests run: 6, Failures: 1. `ipv6LoopbackInBracketsIsLoopback`: expected true but was false. This confirms the observed behaviour (`[::1]` is rejected); whether IPv6 loopback is meant to be accepted is still open (RTM Q1) | `02-red-assertion-failure.txt` |
| Green | Added `host.equals("[::1]")` to the host check | Tests run: 6, Failures: 0. BUILD SUCCESS | `03-green.txt` |
| Refactor | Extracted `isLoopbackHost(String)` helper with a comment; behaviour unchanged | Full suite: Tests run: 78, Failures: 0, Errors: 0. BUILD SUCCESS (72 baseline + 6 new) | `04-refactor-full-suite.txt` |

Application files changed (both in `project/backend`):
- `src/main/java/lk/publiccare/backend/admin/AdminSetupRunner.java`: visibility `private` -> package-private; `[::1]` accepted; helper `isLoopbackHost` extracted.
- New test: `src/test/java/lk/publiccare/backend/admin/AdminSetupRunnerLoopbackTest.java`.

Limitation: runs were on Java 25, not the project's Java 21. The change is in the isolated QA copy only; per the test plan (7.11) the project owner would need to approve it for the original repository.

## Step 1: Inspection of the existing backend JUnit tests

Timing: before choosing the target, the test classes were listed and `ReportServiceTest` and `AuthServiceTest` were looked at in part; the UT-14 gap came from the RTM. The per-class table and the check that no handover test referenced `AdminSetupRunner` (searched in commit `2c1e597`) were completed after the TDD cycle.

Location: `project/backend/src/test/java/lk/publiccare/backend/`. The existing tests were inspected (two classes read in part and one in full; the rest checked by searching for annotations, mocks and test methods) and run unchanged (see `../baseline/`: 72 tests, 0 failures).

| Package | Test class | Tests run (Surefire) | Style |
|---|---|---|---|
| admin | AdminAccountSetupServiceTest | 6 | Mockito unit test |
| admin | AdminSetupConfigurationTest | 1 | Plain JUnit unit test (no Spring context) |
| auth | AuthControllerTest | 7 | `@WebMvcTest` with Spring Security Test |
| auth | AuthServiceTest | 4 | Mockito unit test |
| health | HealthControllerTest | 3 | `@WebMvcTest` |
| report | PublicReportServiceTest | 3 | Mockito unit test |
| report | ReportControllerTest | 25 | `@WebMvcTest` |
| report | ReportDeletionJpaTest | 1 | `@DataJpaTest` on H2 |
| report | ReportDeletionServiceTest | 4 | Mockito unit test |
| report | ReportImageServiceTest | 5 | Mockito unit test |
| report | ReportProgressServiceTest | 7 | Mockito unit test |
| report | ReportServiceTest | 6 | Mockito unit test |
| | **Total** | **72** | |

Findings used to select the TDD target:
- Tools present: JUnit 5, Mockito, Spring Boot test slices, Spring Security Test, H2.
- No existing test referenced `AdminSetupRunner` (searched `src/test` for `AdminSetupRunner` and `isLoopback`: no match before the new test was added). This is the gap UT-14 in `qa/PART01_REQUIREMENTS_AND_RTM.md`.
- The analysis report (section 2.6) suspected that the `::1` check never matches because `URI.getHost()` returns `[::1]`. The code at that time (`AdminSetupRunner.java`, `host.equals("::1")`) agreed with the suspicion, so a failing test was expected but not assumed; it was confirmed by the Red run above.
- The existing tests were not relabelled as TDD. Only `AdminSetupRunnerLoopbackTest` was written test-first.

Note on the count: the QA documents say 73 `@Test` methods. A text search for `@Test` finds 73 matches in the existing tests, but one match is the annotation `@TestConfiguration` at `AuthControllerTest.java:45`, which is not a test method. The real number of test methods, and the number Surefire ran, is 72.
