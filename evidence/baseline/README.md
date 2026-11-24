# Baseline backend test run (Step 1)

Purpose: record the actual state of the existing backend tests before any change.

| Item | Value |
|---|---|
| Date/time of run | 2026-11-24, about 08:30 (+05:30) |
| Command | `.\mvnw.cmd -B test` in `project/backend` (run through Git Bash) |
| Java | Temurin 25.0.5 (the project targets Java 21 and CI will use 21) |
| Maven | 3.9.16 (Maven Wrapper) |
| OS | Windows 11, amd64 |
| Code changes before run | None |
| Result | BUILD SUCCESS: Tests run: 72, Failures: 0, Errors: 0, Skipped: 0 |
| Total time | 01:05 min (includes first-time dependency download) |

Notes:
- The QA documents say 73 `@Test` methods. The Surefire run reports 72 tests. Checked later: a text search for `@Test` also matches `@TestConfiguration` (`AuthControllerTest.java:45`), so the real count is 72. See `../tdd/README.md`, Step 1.
- Tests ran on Java 25, not 21. CI (Java 21) must be run separately.
- Files: `environment.txt` (tool versions), `mvn-test-output.txt` (full console output).
