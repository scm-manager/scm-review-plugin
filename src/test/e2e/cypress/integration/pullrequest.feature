Feature: Pull Requests
  Background:
    Given "scmadmin" is authenticated with password "scmadmin"
    And repository "scmadmin" / "heartofgold" doesnt exist

  Scenario: Creating a pull request
    Given repository "scmadmin" / "heartofgold" exists
    And branch "develop" exists in repository "scmadmin" / "heartofgold" based on branch "master"
    And any commit exists on branch "develop" of repository "scmadmin" / "heartofgold"
    When a pull request is created from "develop" to "master" in repository "scmadmin" / "heartofgold"
    Then a pull request with id 1 exists in repository "scmadmin" / "heartofgold"
