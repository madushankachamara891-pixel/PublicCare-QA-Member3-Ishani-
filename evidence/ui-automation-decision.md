# UI automation decision (Section 3.6)

MEMBER_TASKS.md asks to investigate "UI automation using Selenium, if appropriate". The task prompt says "Prepare API or UI automation". API automation was done (REST Assured, see `api-automation/`). UI automation was investigated and **not implemented**. Nothing in this file is a test result.

## What was inspected

| Item | Finding |
|---|---|
| `project/package.json` | React 19, Vite 8. Scripts: `dev`, `build`, `lint`, `preview`. No test runner, no Vitest/Jest, no Selenium or Playwright |
| `project/src/` | Routing is done by hand from `window.location.pathname` (no router library). The frontend calls the backend over HTTP using cookies and a CSRF header |
| Backend | Needs a database. Tests use H2 in memory. The full application with the frontend is configured for PostgreSQL and a CORS origin |

## Why UI automation was not implemented

1. A browser test needs four things running together: the Vite frontend, the Spring Boot backend, a database and a browser with its driver. The existing automated tests avoid a real database by using H2 with Flyway disabled and with the workaround for the PostgreSQL-generated `reference` column. Wiring the frontend to such a backend would need extra configuration (API base URL, CORS origin, cookies across origins) that was not verified.
2. The CSRF cookie and the `SameSite=Lax` session cookie behave differently when the frontend (Vite dev server) and the backend run on different ports, so this setup could give failures that are not application defects.
3. Selenium is not installed and would add a dependency, a browser and a driver to the project and to CI. CLAUDE.md requires dependencies to be explained and approved first. The value was judged low compared with the API and BDD automation already running against the real backend.
4. The risks that the proposed UI cases (UI-01 to UI-14) check on the client (form validation, role-based landing, admin gate, image preview) are mostly front-end logic that a frontend unit test framework (Vitest) would cover more cheaply. Adding Vitest changes the application project and needs the project owner's approval (test plan section 7.11).

## What is covered instead

- API behaviour that the UI depends on: REST Assured tests (`api-automation/`).
- User workflows (register, log in, submit a report, admin status update) at API level: Cucumber scenarios (`bdd/`).
- The frontend is checked in CI only by `npm run lint` and `npm run build` (once the workflow has run on GitHub). There is no automated test of UI behaviour.

## Limitation to report

UI behaviour requirements such as FR-01.6, FR-02.7, FR-02.9, FR-03.7 and FR-03.8 have no automated test in this work. They remain covered only by the proposed manual cases (MT-xx) and the proposed UI cases (UI-xx), which are NOT EXECUTED.

## If it is done later

A small Playwright or Selenium test (for example: the login page loads and shows its fields) against a running frontend and backend, with the project owner's approval for the new dependency.
