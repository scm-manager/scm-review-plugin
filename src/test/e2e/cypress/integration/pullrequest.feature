Feature: Pull Requests
  Background:
    Given is authenticated
    And repo doesnt exist

  Scenario: Creating a pull request
    Given repository exists
    And develop branch exists
    And commit exists
    When create pr
    Then pr exists
