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

export class GitBuilder {

  private readonly directory = `cypress/fixtures/generated/${this.namespace}-${this.name}`;
  private readonly gitDirectory = `./${this.directory}`;

  constructor(private readonly namespace: string, private readonly name: string, readonly defaultBranch = "main") {
  }

  init() {
    const [protocol, url] = Cypress.config("baseUrl").split("//");
    const credentials = `${Cypress.env("USERNAME")}:${Cypress.env("PASSWORD")}`;
    const urlWithCredentials = `${protocol}//${credentials}@${url}`;
    cy.exec(`git init -b ${this.defaultBranch} ./${this.gitDirectory}`);
    cy.exec(`git -C ${this.gitDirectory} remote add origin "${urlWithCredentials}/repo/${this.namespace}/${this.name}"`);
    return this;
  }

  createAndCommitFile(path: string, content: string, commitMessage: string) {
    cy.writeFile(`${this.directory}/${path}`, content);
    cy.exec(`git -C ${this.gitDirectory} add ${path}`);
    cy.exec(`git -C ${this.gitDirectory} commit -m "${commitMessage}"`);
    return this;
  }

  createAndCheckoutBranch(branchName: string) {
    cy.exec(`git -C ${this.gitDirectory} checkout -b "${branchName}"`);
    return this;
  }

  checkoutBranch(branchName: string) {
    cy.exec(`git -C ${this.gitDirectory} checkout "${branchName}"`);
    return this;
  }

  checkoutDefaultBranch() {
    return this.checkoutBranch(this.defaultBranch);
  }

  deleteBranchLocallyAndRemote(branchName: string) {
    cy.exec(`git -C ${this.gitDirectory} branch -D "${branchName}"`);
    cy.exec(`git -C ${this.gitDirectory} push -d origin "${branchName}"`);
    return this;
  }

  deleteTagLocallyAndRemote(tagName: string) {
    cy.exec(`git -C ${this.gitDirectory} tag -d "${tagName}"`);
    cy.exec(`git -C ${this.gitDirectory} push -d origin "${tagName}"`);
    return this;
  }

  pushAllWithForce() {
    cy.exec(`git -C ${this.gitDirectory} push --all --force origin`);
    return this;
  }

  pushTags() {
    cy.exec(`git -C ${this.gitDirectory} push --tags --force origin`);
    return this;
  }

  rebase(branch: string, onto: string) {
    cy.exec(`git -C ${this.gitDirectory} rebase ${branch} ${onto}`);
    return this;
  }

  rebaseOnto(branch: string) {
    cy.exec(`git -C ${this.gitDirectory} rebase ${branch}`);
    return this;
  }

  tag(tagName: string) {
    cy.exec(`git -C ${this.gitDirectory} tag ${tagName}`);
    return this;
  }

  renameBranchLocallyAndRemote(oldBranchName: string, newBranchName: string) {
    cy.exec(`git -C ${this.gitDirectory} push origin :${oldBranchName} ${newBranchName}`);
    return this;
  }

  amendLatestCommitMessage(newMessage: string) {
    cy.exec(`git -C ${this.gitDirectory} commit --amend -m "${newMessage}"`);
    return this;
  }
}
