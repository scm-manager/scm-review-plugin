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

import { Given, Then } from "cypress-cucumber-preprocessor/steps";

Given("I am authenticated and a repository with a non-default branch exists", () => {
  cy.restLogin(Cypress.env("USERNAME"), Cypress.env("PASSWORD"));
  cy.restCreateRepo("Test", "HeartOfGold");
  cy.restCreateBranch("Test", "HeartOfGold", "develop", "main");
  cy.restCreateFile("Test", "HeartOfGold", "develop", "index.js", "console.log(\"Hello World\"");
  cy.restCreatePr("Test", "HeartOfGold", "develop", "main", "A new PR");
});

Then("Asking the rest api confirms a new pr has been created", () => {
  cy.restGetPr("Test", "HeartOfGold", 1);
});
