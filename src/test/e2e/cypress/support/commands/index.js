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

const restCreateRepo = (namespace, name) => {
  const reposUrl = `http://localhost:8081/scm/api/v2/repositories?initialize=true`;

  cy.request({
    method: "POST",
    url: reposUrl,
    body: {
      name,
      namespace
    }
  });
}

const restCreateBranch = (namespace, name, newBranch, parent = "main") => {
  const createBranchUrl = `http://localhost:8081/scm/api/v2/repositories/${namespace}/${name}/branches`;

  cy.request({
    method: "POST",
    url: createBranchUrl,
    body: {
      name: newBranch,
      parent: parent
    }
  })
}

const restLogin = (username, password) => {
  const loginUrl = `http://localhost:8081/scm/api/v2/auth/access_token`;

  cy.request({
    method: "POST",
    url: loginUrl,
    body: {
      cookie: true,
      username: username,
      password: password,
      grantType: "password"
    }
  });
};

const restCreateFile = (namespace, repo, branch, filename, content, commitMessage = "Created a new file.") => {
  const createFileUrl = `http://localhost:8081/scm/api/v2/edit/${namespace}/${repo}/create`

  cy.request({
    method: "POST",
    url: createFileUrl,
    body: {
      commitMessage: commitMessage,
      branch: branch,
      fileName: filename,
      fileContent: content
    }
  })
}

const restCreatePr = (namespace, repo, source, target, title) => {
  const url = `http://localhost:8081/scm/api/v2/pull-requests/${namespace}/${repo}`

  cy.request({
    method: "POST",
    url: url,
    body: {
      source,
      target,
      title
    }
  })
}

const restGetPr = (namespace, repo, prId = 1) => {
  const url = `http://localhost:8081/scm/api/v2/pull-requests/${namespace}/${repo}/${prId}`

  cy.request({
    method: "GET",
    url: url
  })
}

Cypress.Commands.add("restCreateRepo", restCreateRepo);
Cypress.Commands.add("restLogin", restLogin);
Cypress.Commands.add("restCreateBranch", restCreateBranch);
Cypress.Commands.add("restCreateFile", restCreateFile);
Cypress.Commands.add("restCreatePr", restCreatePr);
Cypress.Commands.add("restGetPr", restGetPr);
