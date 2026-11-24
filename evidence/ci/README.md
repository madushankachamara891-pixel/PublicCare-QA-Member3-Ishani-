# CI evidence: GitHub Actions (Section 3.6)

This is a **remote** run on GitHub, not a local run. The facts below are taken from the GitHub Actions run page (screenshots in this folder, seen on 2026-11-24 about 10:00 local time) and from the workflow file.

| Item | Value |
|---|---|
| Run URL | https://github.com/madushankachamara891-pixel/PublicCare-QA-Member3-Ishani-/actions/runs/45326010526 |
| Workflow | CI (`.github/workflows/ci.yml`), run #1 |
| Trigger | push by Ishani-Kumaranayaka to branch `member3-tdd-bdd-ci` |
| Commit | `70d01e0`, "docs(evidence): record UI automation decision and limitation (section 3.6)" |
| Status | Success |
| Total duration | 1m 6s |
| Job `Backend (Java 21, Maven Wrapper)` | Success, 1m 1s |
| Job `Frontend (Node.js, lint and build)` | Success, 16s |
| Backend test result in the job log | `Tests run: 91, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, total time 49.346 s, finished 2026-11-24T04:24:04Z. Cucumber: `Tests run: 4, Failures: 0` (RunCucumberTest) |
| Artifact | `backend-test-reports`, 364 KB |
| Annotations | 3 warnings (see below) |

Screenshots:
- `github-actions-run-1-summary.png`: run summary and the two successful jobs.
- `github-actions-run-1-annotations-artifacts.png`: annotations and the uploaded artifact.
- `github-actions-run-1-backend-log-91-tests.png`: the backend job log with the Maven test summary (91 tests, 0 failures, BUILD SUCCESS).

The UI job (Selenium) was added to the workflow after run #1, so it is not part of this run.

## What the workflow runs

| MEMBER_TASKS.md stage | Step in `ci.yml` |
|---|---|
| Checkout | `actions/checkout@v4` |
| Java 21 | `actions/setup-java@v4`, Temurin 21, Maven cache |
| Backend tests with the Maven Wrapper | `./mvnw -B test` in `project/backend` (existing JUnit tests, TDD test, REST Assured API tests, Cucumber scenarios) |
| Node.js | `actions/setup-node@v4`, Node 22 |
| Frontend dependencies | `npm ci` in `project` |
| Lint | `npm run lint` |
| Build | `npm run build` |
| Upload reports | `actions/upload-artifact@v4`, Surefire and Cucumber reports, `if: always()` |

## Warnings shown on the run page

- Node.js 20 is deprecated: `actions/checkout@v4`, `actions/setup-node@v4` and `actions/setup-java@v4` target Node.js 20 and are forced to run on Node.js 24.
- `setup-java v4 is deprecated and will no longer receive updates. Please migrate to actions/setup-java@v5.`

The run passed despite these warnings. They are not fixed in this work; updating the action versions is a possible follow-up.

## Notes and remaining limitations

- The 91 tests that ran on GitHub (Java 21) match the 91 tests of the local run on Java 25: 72 existing + 6 TDD + 4 Cucumber + 9 REST Assured. This is the first run on Java 21. The artifact was downloaded and read (`artifact-summary.txt`): 15 test classes, 91 tests, 0 failures, 0 errors, 0 skipped, Java 21.0.13 (Eclipse Adoptium) on Linux. Its SHA-256 starts with the digest shown on the run page. The ZIP itself is not committed.
- The frontend job result (lint and build passed) is read from the job status on the run page. Its log was not inspected, so no lint warning count or build output is recorded here.
- No PostgreSQL was used in CI; the backend tests use H2 as in the local runs. PostgreSQL-specific behaviour (for example the generated `reference` column, INT-01) is not covered.
- Only run #1 is documented. Later runs triggered by further pushes are not recorded here.
