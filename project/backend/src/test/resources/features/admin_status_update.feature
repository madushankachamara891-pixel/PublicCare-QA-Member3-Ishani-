Feature: Administrator updates a report status
  As an administrator
  I want to change the status of a citizen report and add a note
  So that the citizen can follow the progress of their report

  # Requirements: FR-06.6, FR-05.2, FR-02.8, FR-08.5 (role separation)
  # All accounts and data below are synthetic. The administrator is created
  # directly in the test database because no web route creates admins (FR-06.9).

  Scenario: Administrator moves a pending report to in progress with a note
    Given a citizen has submitted a report titled "Pothole near the school"
    And an administrator is logged in
    When the administrator sets the report status to "IN_PROGRESS" with the note "Repair crew assigned"
    Then the status update is accepted
    And the citizen sees the report status "IN_PROGRESS"
    And the citizen sees the administrator note "Repair crew assigned"

  Scenario: A citizen cannot change the status of a report
    Given a citizen has submitted a report titled "Overflowing bin"
    When the citizen tries to set the report status to "RESOLVED"
    Then the request is rejected because access is denied
    And the report status is still "PENDING" for the citizen
