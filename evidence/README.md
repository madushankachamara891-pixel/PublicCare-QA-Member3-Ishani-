# Member 3 evidence index: Sections 3.5 and 3.6

Scope: TDD, BDD, test automation and CI/CD for the isolated PublicCare QA copy (see `MEMBER_TASKS.md`). Everything below comes from actual runs recorded in this folder. Local runs: Windows 11, Java 25.0.5, Maven 3.9.16 wrapper, 2026-11-24. The CI run: GitHub Actions, Java 21, same day.

## Deliverables (MEMBER_TASKS.md) and where they are

| Deliverable | Status | Location |
|---|---|---|
| TDD Red-Green-Refactor evidence | Done | `tdd/` (README, compile error, assertion failure, green, full suite after refactor) |
| Gherkin feature files | Done | `project/backend/src/test/resources/features/citizen_report.feature`, `admin_status_update.feature` (4 scenarios) |
| Step definitions | Done | `project/backend/src/test/java/lk/publiccare/backend/bdd/` (`PublicCareSteps.java`, `CucumberSpringConfiguration.java`, `RunCucumberTest.java`) |
| Cucumber execution evidence | Done | `bdd/` (runs, HTML/JSON/XML reports, README). Also in the CI job log: `ci/github-actions-run-1-backend-log-91-tests.png` |
| API automation tests | Done (partial coverage) | `project/backend/src/test/java/lk/publiccare/backend/api/PublicCareApiTest.java` (9 REST Assured tests); evidence in `api-automation/` |
| UI automation tests | Done (partial coverage) | `project/backend/src/test/java/lk/publiccare/backend/ui/PublicCareUiIT.java` (6 Selenium tests); evidence in `ui-automation/`; decision history in `ui-automation-decision.md` |
| GitHub Actions YAML | Done | `.github/workflows/ci.yml` |
| Actual CI execution | Done | Run URL and screenshots in `ci/` (run #1, success): https://github.com/madushankachamara891-pixel/PublicCare-QA-Member3-Ishani-/actions/runs/45326010526 |
| Test reports | Done | `bdd/cucumber-reports/`, `api-automation/surefire-reports/`, the CI artifact `backend-test-reports` (read and summarised in `ci/artifact-summary.txt`; the ZIP is not committed) |
| Limitations and explanations | Done | This file and the README in each folder |

## Results summary

| Run | Environment | Result |
|---|---|---|
| Baseline, before any change (`baseline/`) | Local, Java 25 | 72 tests, 0 failures |
| After TDD (`tdd/`) | Local, Java 25 | 78 tests, 0 failures (72 + 6 new) |
| After BDD (`bdd/`) | Local, Java 25 | 82 tests, 0 failures (+4 Cucumber scenarios) |
| After API automation (`api-automation/`) | Local, Java 25 | 91 tests, 0 failures (+9 REST Assured tests) |
| Final backend run after wording corrections (`final/`) | Local, Java 25 | 91 tests, 0 failures |
| Frontend `npm ci`, lint, build (`frontend/`) | Local, Node 24.13.0 | All exit code 0; npm reported 3 moderate vulnerabilities (not investigated) |
| UI tests, Selenium (`ui-automation/`) | Local, Java 25, Chrome 157 | 6 tests, 0 failures (run separately from the 91) |
| GitHub Actions run #1 (`ci/`) | GitHub, Java 21 | Backend job: 91 tests, 0 failures, BUILD SUCCESS. Frontend job (npm ci, lint, build): success |

Failed and corrected runs are kept as evidence: Cucumber 7.34.9 incompatible with Spring Boot's JUnit 5.12.2 (`bdd/01a`), CSRF cookie error in test code (`bdd/02a`), stale Maven `target/` build failure (`api-automation/01a`), and a failing duplicate-email expectation (`api-automation/01b`).

## Findings

| Finding | Where | Status |
|---|---|---|
| Bracketed IPv6 loopback `[::1]` was rejected by `AdminSetupRunner.isLoopbackPostgres` (suspected in the analysis report, section 2.6) | `tdd/` | Behaviour confirmed by a failing test and changed in this QA copy. Whether IPv6 loopback is meant to be accepted is still open (RTM Q1) |
| A duplicate email with surrounding spaces returns 400, not the 409 proposed in API-04 (observed; likely because `@Email` validation runs before trimming, not confirmed from the response body) | `api-automation/README.md` | Expected result needs clarification (not yet listed among the RTM open questions); not a confirmed defect |
| The QA documents count 73 test methods; the real count is 72 (`@TestConfiguration` matches a text search for `@Test`) | `tdd/README.md`, `baseline/README.md` | Explained |

## Dependencies and configuration changes

| Change | File | Why |
|---|---|---|
| `cucumber-bom` 7.23.0; test-scope `cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`, `junit-platform-suite` | `project/backend/pom.xml` | BDD. 7.34.9 was tried first and failed; 7.23.0 matches the JUnit version managed by Spring Boot 3.5.16 |
| Test-scope `io.rest-assured:rest-assured` (version managed by Spring Boot, 5.5.7) | `project/backend/pom.xml` | API automation |
| Test-scope `selenium-java` (version managed by Spring Boot, 4.31.0) | `project/backend/pom.xml` | UI automation |
| New GitHub Actions workflow; the UI job was added after run #1 | `.github/workflows/ci.yml` | CI |
| No frontend dependency or script was added | `project/package.json` unchanged | |

## Application source code changed

Only `project/backend/src/main/java/lk/publiccare/backend/admin/AdminSetupRunner.java`: `isLoopbackPostgres` changed from private to package-private so it can be tested, `[::1]` accepted, and a helper `isLoopbackHost` extracted. All other changes are test code, test resources, `pom.xml`, the workflow and evidence. The test plan (section 7.11) says changes to the original application need the project owner's approval; this change is only in the QA copy.

## Limitations

- Local runs used Java 25; the project targets Java 21. The CI run (Java 21) gave the same 91 passing tests.
- All backend automation (Cucumber, REST Assured, existing JPA test) uses H2, not PostgreSQL. The database-generated `reference` column is not exercised: the `PC-########` checks pass through the service's fallback format. INT-01 (Flyway on PostgreSQL) is not done.
- Cucumber scenarios use MockMvc, without a server or browser. REST Assured tests use a real server on a random port, still on H2.
- Coverage is partial: 4 BDD scenarios, and 9 REST Assured tests covering parts of API-01, 02, 03, 04, 08, 09, 10, 15. Image upload, public feed, deletion, progress updates and most of the 25 proposed API cases are not automated.
- UI automation covers 6 of the 14 proposed UI cases (UI-01 to UI-05, UI-13) in Chrome only. FR-03.7 (image picker) has no automated test. There are no frontend unit tests (no Vitest). The UI job in the workflow is newer than run #1, so no CI result for it is recorded here.
- The CI run was documented from screenshots of the GitHub run page. The backend artifact was downloaded and read. The frontend job log was not inspected. Only run #1 is documented.
- The workflow uses action versions that GitHub flags as deprecated (Node.js 20 runtime; `setup-java@v4`). The run still passed.
- In `qa/`, the 15 test cases that were executed in full or in part were updated with actual results. The other 64 cases and the requirement-level RTM rows (TASK 5) still say PROPOSED / NOT EXECUTED.
- No code coverage tool (such as JaCoCo) was added, so no coverage figure is reported.
