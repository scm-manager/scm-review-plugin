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

Given("repository {string} / {string} doesnt exist", (namespace, repo) => {
  cy.restDeleteRepo(namespace, repo)
});

Given("{string} is authenticated with password {string}", (username, password) => {
  cy.restLogin(username, password);
});

Given("repository {string} / {string} exists", (namespace, repo) => {
  cy.restCreateRepo(namespace, repo);
});

Given("branch {string} exists in repository {string} / {string} based on branch {string}", (branch, namespace, repo, parent) => {
  cy.restCreateBranch(namespace, repo, branch, parent);
});

Given("any commit exists on branch {string} of repository {string} / {string}", (branch, namespace, repository) => {
  cy.restCreateFile(namespace, repository, branch, "index.js", "console.log(\"Hello World\"");
});

When("a pull request is created from {string} to {string} in repository {string} / {string}", (from, to, namespace, repository) => {
  cy.restCreatePr(namespace, repository, from, to, "A new PR");
});

Then("a pull request with id {int} exists in repository {string} / {string}", (id, namespace, repository) => {
  cy.restGetPr(namespace, repository, id);
});
