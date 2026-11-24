# API automation evidence: REST Assured (Section 3.6)

Date: 2026-11-24. Environment: Windows 11, Temurin Java 25.0.5 (project targets Java 21), Maven 3.9.16 (wrapper), Spring Boot 3.5.16, REST Assured 5.5.7 (version managed by Spring Boot), in-memory H2 database.

## What was built

| Item | File |
|---|---|
| API test class (9 tests) | `project/backend/src/test/java/lk/publiccare/backend/api/PublicCareApiTest.java` |
| Build change | `project/backend/pom.xml`: test-scope `io.rest-assured:rest-assured` (no version, managed by Spring Boot) |

Tool choice: REST Assured, because the test plan (section 7.5) proposes it, it is written in Java next to the existing JUnit tests, and Spring Boot manages a compatible version. Postman/Newman was not used.

How it runs: `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts the real application with a real Tomcat on a random port, on H2 (Flyway off, Hibernate creates the schema, same approach as the existing `ReportDeletionJpaTest`). REST Assured sends real HTTP requests and keeps the `JSESSIONID` cookie. For writes it sends the raw `XSRF-TOKEN` cookie and the masked token from `/api/auth/csrf` as the `X-XSRF-TOKEN` header, as the frontend does. The administrator is inserted directly into the test database because no web route creates admins (FR-06.9). All data is synthetic. No application source code was changed for API automation.

This differs from the Cucumber scenarios (section 3.5), which use MockMvc without a server.

## Tests and the proposed QA cases they cover

| Test method | QA case (RTM TASK 6-C) | Checks | Result |
|---|---|---|---|
| csrfEndpointReturnsTokenAndCookie | API-01 | 200, `token` in body, `XSRF-TOKEN` cookie | PASS |
| registrationForcesCitizenRoleAndHidesPassword | API-02 | body contains `"role":"ADMIN"`; 201, role `CITIZEN`, no `password`/`passwordHash` field | PASS |
| passwordLengthBoundary | API-03 (part only) | 11-character password 400, 12-character password 201 | PASS |
| duplicateEmailWithDifferentCaseIsRejected | API-04 (case) | upper-case duplicate gives 409 | PASS |
| emailWithSurroundingSpacesIsRejectedByValidation | API-04 (padding) | padded duplicate gives 400 (observed, see finding) | PASS (characterization) |
| meRequiresSession | API-08 (part) | `/me` 401 without session with body `Authentication required.`, 200 with session | PASS |
| adminEndpointAuthorisationMatrix | API-09 (one endpoint) | `GET /api/admin/reports`: anonymous 401, citizen 403, admin 200 | PASS |
| createReportUsesSessionOwner | API-10 | body has `ownerId` 999999; 201, `PENDING`, reference matches `^PC-\d{8}$`, owner is the session user | PASS |
| otherUsersReportLooksLikeMissingReport | API-15 (GET only) | another user's report and a missing report: both 404 with identical bodies; owner gets 200 | PASS |

Only part of the 25 proposed API cases is automated. Not automated: API-03 other boundaries, API-05 (CSRF on every write), API-06, API-07, API-09 other endpoints, API-11, API-12 to API-14 (images), API-16 to API-25.

## Actual results

| Run | Command | Result | File |
|---|---|---|---|
| 1 | `.\mvnw.cmd -B test -Dtest=PublicCareApiTest` | Build failed before any test ran: `bad class file ... NoSuchFileException` in `target/classes`. The error was at the `testCompile` step. Likely cause (inferred, not proven): class files missing from `target/classes` after the `pom.xml` change; the same command with `clean` then compiled the same test code and ran | `01a-run1-stale-target-build-failure.txt` |
| 2 | `.\mvnw.cmd -B clean test -Dtest=PublicCareApiTest` | 8 tests, 1 failure: `duplicateEmailIsRejected` expected 409 but was 400 | `01b-run2-fail-duplicate-email-400.txt` |
| 3 | `.\mvnw.cmd -B test -Dtest=PublicCareApiTest` | PASS: Tests run 9, Failures 0, Errors 0, Skipped 0 (the failing test was split into the two API-04 tests above) | `02-run3.txt` |
| Full suite | `.\mvnw.cmd -B test` | BUILD SUCCESS: Tests run 91, Failures 0 (72 existing + 6 TDD + 4 Cucumber + 9 API) | `03-full-suite.txt` |

Surefire report for the API class: `surefire-reports/`.

## Finding: padded email gets 400, not 409 (API-04)

The proposed API-04 expects 409 for a duplicate email "with different case or padding". Run 2 showed the padded case returns 400. Observed (confirmed by the split test): the padded email returns 400 and the different-case duplicate returns 409. Likely reason (from reading the code; the 400 response body was not inspected): `RegisterRequest.email` is validated with `@Email` before `AuthService.register` trims it, so an address with leading or trailing spaces fails validation.

This is either an expected-result error in the test case or a behaviour that needs clarification with the project owner (not yet listed among the RTM open questions). It is recorded here as a finding for the defect log, not as a confirmed defect. The test `emailWithSurroundingSpacesIsRejectedByValidation` documents the observed behaviour; it would need to change if the behaviour changes. FR-01.3 says the email is trimmed and lower-cased, which holds for the service but cannot be reached with padded input through the API.

## Limitations

- Run on Java 25, not Java 21. The Java 21 run was done later in CI (see `../ci/`): 91 tests, 0 failures.
- H2 in MODE=PostgreSQL, not PostgreSQL. The `reference` check (`PC-########`) passes on H2 through the service's fallback format, because H2 does not generate the `reference` column that PostgreSQL generates (same limitation as the BDD evidence). It does not verify the database-generated value.
- Image upload, public feed, deletion and progress-update endpoints are not covered.
- No UI automation was done for this section yet; the decision on Selenium or Playwright is a separate step.
