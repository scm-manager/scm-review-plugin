/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
