Feature: Pull Request
  I want to manage pull requests.

  Scenario: Creating a pull request
    Given I am authenticated and a repository with a non-default branch exists
    Then Asking the rest api confirms a new pr has been created
