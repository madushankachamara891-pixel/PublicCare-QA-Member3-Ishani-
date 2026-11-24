# Frontend install, lint and build (local run)

Date: 2026-11-24. Windows 11, Node v24.13.0, npm 11.11.0 (CI uses Node 22). Run in `project/`. No source file, `package.json` or `package-lock.json` was changed.

| Command | Exit code | Actual output | File |
|---|---|---|---|
| `npm ci` | 0 | 143 packages added. npm reported 3 moderate severity vulnerabilities | `01-npm-ci.txt` |
| `npm run lint` | 0 | ESLint printed no errors or warnings | `02-npm-lint.txt` |
| `npm run build` | 0 | Vite 8.3.5, 46 modules transformed, built in 236 ms | `03-npm-build.txt` |

Notes:
- The 3 vulnerabilities were reported by npm during install. They were not investigated (`npm audit` was not run) and not fixed, because that would change dependencies.
- Lint and build do not test UI behaviour. There are no frontend tests.
- This is a local run. It is not evidence for the CI frontend job.
