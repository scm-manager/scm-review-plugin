/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import { hri } from "human-readable-ids";
import { GitBuilder } from "./__helpers/git-builder";
// eslint-disable-next-line no-restricted-imports
import { withAuth } from "@scm-manager/integration-test-runner/build/lib/helpers";

describe("Pull Request Details", () => {
  const createPullRequest = (namespace: string, name: string, from: string, to: string, title: string) =>
    cy
      .request(
        withAuth({
          method: "POST",
          url: `/api/v2/pull-requests/${namespace}/${name}`,
          headers: {
            "Content-Type": "application/vnd.scmm-pullRequest+json;v=2"
          },
          body: {
            source: from,
            target: to,
            title
          }
        })
      )
      .then(response =>
        cy
          .request(response.headers["location"] as string)
          .then(locationResponse => cy.wrap(locationResponse.body.id).as("pullRequestId"))
      );

  const visitPullRequest = (namespace: string, name: string, pullRequestId: string) =>
    cy.visit(`/repo/${namespace}/${name}/pull-request/${pullRequestId}`);

  beforeEach(() => {
    const namespace = hri.random();
    const repoName = hri.random();
    cy.wrap(namespace).as("namespace");
    cy.wrap(repoName).as("repoName");
    cy.restCreateRepo("git", namespace, repoName);
    cy.restLogin("scmadmin", "scmadmin");
  });

  describe("Branch deletion", () => {
    beforeEach(function() {
      cy.wrap(
        new GitBuilder(this.namespace, this.repoName)
          .init()
          .createAndCommitFile("README.md", "This project is cool", "Initial commit")
          .pushAllWithForce()
          .createAndCheckoutBranch("develop")
          .createAndCommitFile("LICENSE.md", "Super safe license", "Add license")
          .pushAllWithForce()
          .createAndCheckoutBranch("feature")
          .createAndCommitFile("code.js", "console.log('Hello World');", "Cool new change")
          .pushAllWithForce()
          .checkoutBranch("main")
      ).as("git");
      createPullRequest(this.namespace, this.repoName, "feature", "develop", "Test PR");
    });

    it("should work when source branch is deleted", function() {
      // When
      this.git.deleteBranchLocallyAndRemote("feature");
      visitPullRequest(this.namespace, this.repoName, this.pullRequestId);

      // Then
      cy.contains("REJECTED").should("exist");
      cy.contains("The source branch of the pull request has been deleted.").should("exist");
    });

    it("should work when target branch is deleted", function() {
      // When
      this.git.deleteBranchLocallyAndRemote("develop");
      visitPullRequest(this.namespace, this.repoName, this.pullRequestId);

      // Then
      cy.contains("REJECTED").should("exist");
      cy.contains("The target branch of the pull request has been deleted.").should("exist");
    });
  });
});
