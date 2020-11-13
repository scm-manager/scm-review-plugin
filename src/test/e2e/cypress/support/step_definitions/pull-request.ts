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

Given("repo doesnt exist", () => {
  cy.restDeleteRepo(Cypress.env("USERNAME"), "heartofgold")
});

Given("is authenticated", () => {
  cy.restLogin(Cypress.env("USERNAME"), Cypress.env("PASSWORD"));
});

Given("repository exists", () => {
  cy.restCreateRepo("test", "heartofgold");
});

Given("develop branch exists", () => {
  cy.restCreateBranch(Cypress.env("USERNAME"), "heartofgold", "develop", "master");
});

Given("commit exists", () => {
  cy.restCreateFile(Cypress.env("USERNAME"), "heartofgold", "develop", "index.js", "console.log(\"Hello World\"");
});

When("create pr", () => {
  cy.restCreatePr(Cypress.env("USERNAME"), "heartofgold", "develop", "master", "A new PR");
});

Then("pr exists", () => {
  cy.restGetPr(Cypress.env("USERNAME"), "heartofgold", 1);
});
