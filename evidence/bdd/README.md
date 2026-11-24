# BDD evidence: Cucumber scenarios (Section 3.5)

Date: 2026-11-24. Environment: Windows 11, Temurin Java 25.0.5 (project targets Java 21), Maven 3.9.16 (wrapper), Spring Boot 3.5.16, JUnit Platform 1.12.2, Cucumber-JVM 7.23.0, in-memory H2 database.

## What was built

| Item | File |
|---|---|
| Feature 1 (2 scenarios) | `project/backend/src/test/resources/features/citizen_report.feature` |
| Feature 2 (2 scenarios) | `project/backend/src/test/resources/features/admin_status_update.feature` |
| Step definitions | `project/backend/src/test/java/lk/publiccare/backend/bdd/PublicCareSteps.java` |
| Spring Boot test configuration | `.../bdd/CucumberSpringConfiguration.java` |
| JUnit Platform runner | `.../bdd/RunCucumberTest.java` |
| Build change | `project/backend/pom.xml`: `cucumber-bom` 7.23.0 and test-scope `cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`, `junit-platform-suite` |

Scenarios and the requirements they trace to:

| # | Scenario | Requirements |
|---|---|---|
| 1 | A new citizen registers, logs in and submits a report | FR-01.1, FR-01.5, FR-02.2, FR-03.1, FR-03.3 |
| 2 | A visitor who is not logged in cannot submit a report | FR-02.8 |
| 3 | Administrator moves a pending report to in progress with a note | FR-06.6, FR-05.2 |
| 4 | A citizen cannot change the status of a report | FR-02.8, FR-08.5 (role separation) |

How the scenarios run: the real PublicCare application (controllers, Spring Security with session and CSRF, services, JPA, BCrypt) is started once with `@SpringBootTest` on H2. Requests are sent with MockMvc. The CSRF token is fetched from `/api/auth/csrf` and sent as a cookie and header, as the frontend does. The administrator is inserted directly into the test database because no web route creates admins (FR-06.9). All accounts and report data are synthetic. No application source code was changed for BDD.

## Actual results

| Run | Command | Result | File |
|---|---|---|---|
| Dependency check, Cucumber 7.34.9 | `.\mvnw.cmd -B test` | FAILED: `NoClassDefFoundError: DiscoveryIssueReporter`. 7.34.9 is built for JUnit 5.14; Spring Boot 3.5.16 manages JUnit 5.12.2. Zero tests ran | `01a-fail-cucumber-7.34.9.txt` |
| Dependency check, Cucumber 7.23.0 | `.\mvnw.cmd -B test` | BUILD SUCCESS, 78 tests, 0 failures (no Cucumber features existed yet) | `01b-pom-deps-regression-7.23.0.txt` |
| Cucumber run 1 | `.\mvnw.cmd -B test -Dtest=RunCucumberTest` | FAILED: 4 scenarios, 4 failures, `expected 201 but was 403`. Cause: the test code sent the masked token from the response body as the CSRF cookie. Spring Security 6 expects the raw token from the `Set-Cookie` header in the cookie. This was a test code error, not an application defect | `02a-run1-fail-csrf-403.txt` |
| Cucumber run 2 | `.\mvnw.cmd -B test -Dtest=RunCucumberTest` | PASS: Tests run 4, Failures 0, Errors 0, Skipped 0 | `02b-run2.txt` |
| Full suite | `.\mvnw.cmd -B test` | BUILD SUCCESS: Tests run 82, Failures 0 (72 existing + 6 TDD + 4 Cucumber) | `03-full-suite.txt` |

Cucumber reports from the full-suite run: `cucumber-reports/cucumber.html`, `cucumber.json`, `cucumber.xml` (copied from `project/backend/target/cucumber-reports/`).

## Limitations

- Run on Java 25, not the project's Java 21. The Java 21 run was done later in CI (see `../ci/`): 91 tests, 0 failures.
- H2 in MODE=PostgreSQL, not PostgreSQL. Flyway migrations are disabled and Hibernate creates the schema, as in the existing `ReportDeletionJpaTest`. The test runs `alter table reports alter column reference drop not null` for the same reason as that test.
- The reference check ("starts with PC-") passed on H2 through the service's fallback format (`PC-` plus an 8-digit id), because H2 does not generate the `reference` column that PostgreSQL generates. It does not verify the database-generated value; that needs a PostgreSQL integration test (INT-01, not done).
- Requests go through MockMvc, not a real HTTP server, and there is no browser. UI behaviour (FR-01.6, FR-02.7) is not covered by these scenarios.
- The Cucumber version 7.23.0 was chosen for compatibility with JUnit 5.12.2, not because it is the newest release.
- Only 4 scenarios and 2 workflows (register/login/report and admin status update) are automated. Other requirements are not covered by BDD.
