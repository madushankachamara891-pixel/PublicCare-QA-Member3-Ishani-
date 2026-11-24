# UI automation evidence: Selenium (Section 3.6)

Date: 2026-11-24. Environment: Windows 11, Java 25.0.5, Selenium 4.31.0 (version managed by Spring Boot 3.5.16), Chrome 157 headless, ChromeDriver 157.0.8103.78 (downloaded by Selenium Manager), in-memory H2.

## What was built

| Item | File |
|---|---|
| UI test class (6 tests) | `project/backend/src/test/java/lk/publiccare/backend/ui/PublicCareUiIT.java` |
| Build change | `project/backend/pom.xml`: test-scope `selenium-java` |
| CI job | `.github/workflows/ci.yml`: job `UI tests (Selenium, headless Chrome)` |

The backend runs on port 8080, the address built into the frontend. The built frontend (`project/dist`) is served on port 5199 by a small server inside the test. Port 5173 was already used by another program on the test machine. Each test starts a new browser with an empty profile. No application or frontend source code was changed. All data is synthetic.

Run it with `npm ci` and `npm run build` in `project/`, then `.\mvnw.cmd -B test -Dtest=PublicCareUiIT` in `project/backend/`. The name ends in `IT`, so the normal `mvnw test` (91 tests) does not include it.

## Tests

| Test method | QA case | Checks | Result |
|---|---|---|---|
| registrationBoundariesAndMismatch | UI-01 | Name holds at most 120 characters; password 11 blocked; 12 and 72 create an account, show the success message and empty the email field; mismatch blocked with "Passwords do not match." | PASS |
| loginRedirectsByRole | UI-02 | Citizen to `/my-reports`, log out through the account menu, admin to `/admin` | PASS |
| failedLoginShowsGenericAlert | UI-03 | Wrong password and unknown email both show "Invalid email or password." in `role=alert`; user stays signed out | PASS |
| signedOutUserCannotSubmitReport | UI-04 | Submit button disabled; sign-in notice and link visible | PASS |
| requiredFieldsAndSuccessfulReport | UI-05 | Each of the 4 empty required fields blocks the submit; valid report shows reference `PC-########` | PASS |
| adminPagesAreGatedForCitizen | UI-13 | `/admin`, `/admin/issues` and `/admin/update-issue` show "Administrator access required" | PASS |

## Actual results

| Run | Result | File |
|---|---|---|
| Normal suite with Selenium added, run 1 | FAILED, 0 tests ran: class files were missing from `target/test-classes` during the run. Likely cause (not proven): another program, such as the VS Code Java extension, rebuilding the project after the `pom.xml` change | `01a-run1-missing-class-files.txt` |
| Normal suite, clean build | 91 tests, 0 failures | `01b-regression-with-selenium.txt` |
| UI tests, run 1 | 6 tests, 0 failures | `02-ui-run1.txt` |
| Fault check | 6 tests, 2 failures, as intended. Two texts were changed on purpose in the build output only (`dist`), and the two tests that check them failed. `dist` was rebuilt afterwards | `03-fault-check-expected-failures.txt` |
| UI tests, final | 6 tests, 0 failures | `04-ui-final.txt` |
| Normal suite, final | 91 tests, 0 failures | `05-full-suite.txt` |

Screenshots of the last page of each test: `screenshots/`. Surefire summary: `surefire-reports/`.

## Limitations

- Local results only. The GitHub Actions job was added but its result is not recorded here.
- 6 of the 14 proposed UI cases are automated (UI-01 to UI-05 and UI-13). UI-06 to UI-12 and UI-14 are not.
- "No request was sent" is checked by confirming that nothing was stored after a short wait, not by inspecting network traffic. For UI-01, form reset is checked on the email field only.
- Chrome only. No other browser and no mobile width was tested.
- H2, not PostgreSQL. The report reference passes through the service's fallback format.
- Tests use fixed ports 8080 and 5199, which must be free.
