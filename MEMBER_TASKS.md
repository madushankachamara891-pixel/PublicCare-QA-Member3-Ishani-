# Member 3 - TDD, BDD, Automation and CI/CD

## Assigned sections

- Section 3.5 - Test-Driven Development and Behavior-Driven Development
- Section 3.6 - Test Automation and CI/CD

## Objective

Demonstrate practical testing and automation using the isolated PublicCare copy.

## Section 3.5 - TDD

1. Inspect existing backend JUnit tests.
2. Identify a suitable method or requirement for a new test.
3. Demonstrate the Red-Green-Refactor cycle.
4. Record the test written before implementation.
5. Show the failing test result.
6. Show the implementation and passing result.
7. Explain refactoring, if performed.

Do not label an existing test as TDD unless the development sequence is demonstrated.

## Section 3.5 - BDD

1. Select 1-2 realistic PublicCare user scenarios.
2. Write Gherkin Feature and Scenario files.
3. Use Given, When and Then correctly.
4. Implement the step definitions using a compatible Cucumber setup.
5. Execute the scenarios if the environment permits.

Possible scenarios:
- Citizen registers and logs in.
- Citizen submits a report.
- Administrator updates a report status.

Do not claim Cucumber scenarios are automated until they have actually been executed.

## Section 3.6 - Test automation

Investigate suitable automation for:

- Backend API testing using REST Assured or another justified tool.
- UI automation using Selenium, if appropriate.
- Existing JUnit backend tests.

The frontend currently uses React and Vite. Inspect package.json before selecting a frontend test framework.

## CI/CD

Create a GitHub Actions workflow in your own QA copy.

Suggested workflow stages:
1. Checkout
2. Set up Java 21
3. Run backend tests
4. Set up Node.js
5. Install frontend dependencies
6. Run frontend lint
7. Run frontend build
8. Upload relevant test reports, if configured

Use the Maven Wrapper.

A successful workflow must be demonstrated using an actual GitHub Actions run. Do not use a local screenshot as proof of a remote CI run.

## Deliverables

- TDD Red-Green-Refactor evidence
- Gherkin feature files
- Step definitions
- Cucumber execution evidence
- API/UI automation tests
- GitHub Actions YAML
- Actual CI execution screenshot or run URL
- Test reports
- Limitations and explanations

## Constraints

- Work only in your Member3 project copy.
- Do not modify the original project.
- Do not commit secrets or credentials.
- Do not invent CI results.
- Record dependencies added and configuration changes.
