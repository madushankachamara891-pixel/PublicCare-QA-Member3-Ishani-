Feature: Citizen registers and submits a report
  As a citizen
  I want to register, log in and submit a public issue report
  So that the authorities can review and resolve the issue

  # Requirements: FR-01.1, FR-01.5, FR-02.2, FR-03.1, FR-03.3
  # All accounts and data below are synthetic.

  Scenario: A new citizen registers, logs in and submits a report
    Given a visitor registers with the name "Nimali Perera" and a unique email
    And the registration is accepted with the role "CITIZEN"
    And the citizen logs in with the registered email and password
    When the citizen submits a report titled "Broken streetlight" in the category "Streetlights" at "Galle Road, Colombo"
    Then the report is created
    And the report status is "PENDING"
    And the report reference starts with "PC-"
    And the report belongs to the logged in citizen

  Scenario: A visitor who is not logged in cannot submit a report
    Given a visitor who is not logged in
    When the visitor submits a report titled "Blocked drain" in the category "Drainage" at "Main Street"
    Then the request is rejected because authentication is required
