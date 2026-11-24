# UI automation decision (Section 3.6)

**Current decision (2026-11-24): implemented.** Six proposed UI cases are automated with Selenium. Tests, results and limitations: `ui-automation/README.md`.

## History

1. **First decision:** not implemented. API automation already met "Prepare API or UI automation", and a browser test needs the frontend, the backend, a database and a browser running together, plus a new dependency.
2. **Revised:** a check of the project showed that the setup was practical. Form fields have stable IDs, Spring Boot manages a Selenium version, the built frontend can be served from inside the test, and H2 can replace PostgreSQL. The new dependency and CI job were approved before they were added.

An earlier version of this file gave a wrong reason about `SameSite` cookies and different ports. `SameSite` does not depend on the port, and the tests run with the frontend and backend on different ports.
