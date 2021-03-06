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

import fetchMock from "fetch-mock";
import {
  createChangesetUrl,
  createPullRequest,
  getBranches,
  getPullRequest,
  getPullRequests,
  createPullRequestComment,
  merge,
  updatePullRequestComment
} from "./pullRequest";
import { BasicComment, BasicPullRequest, PullRequest } from "./types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { Comment } from "./types/PullRequest";
import { Comments } from "./types/PullRequest";

describe("API create pull request", () => {
  const PULLREQUEST_URL = "/repositories/scmadmin/TestRepo/newPullRequest";

  const BRANCH_URL = "/repositories/scmadmin/TestRepo/branches";

  const pullRequest: BasicPullRequest = {
    source: "sourceBranch",
    target: "targetBranch",
    title: "This is a title"
  };

  const branchRequest = {
    _embedded: {
      branches: [
        {
          name: "branchA",
          defaultBranch: false
        },
        {
          name: "branchB",
          defaultBranch: true
        }
      ]
    }
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should create pull request successfully", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 204,
      headers: { Location: "pullrequest/1" }
    });
    fetchMock.getOnce("/api/v2/pullrequest/1?fields=id", {
      status: 200,
      body: { id: 1 }
    });

    createPullRequest(PULLREQUEST_URL, pullRequest).then(id => {
      expect(id).toBe(1);
      done();
    });
  });

  it("should fail on creating pull request", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    createPullRequest(PULLREQUEST_URL, pullRequest).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });

  it("should get branches successfully", done => {
    fetchMock.getOnce("/api/v2" + BRANCH_URL, branchRequest);

    getBranches(BRANCH_URL).then(response => {
      expect(response).toEqual({
        branchNames: ["branchA", "branchB"],
        defaultBranch: {
          name: "branchB",
          defaultBranch: true
        }
      });
      done();
    });
  });

  it("should fail on getting branches", done => {
    fetchMock.getOnce("/api/v2" + BRANCH_URL, {
      status: 500
    });

    getBranches(BRANCH_URL).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API update pull request", () => {
  const PULLREQUEST_URL = "/repositories/scmadmin/TestRepo/newPullRequest";

  const pullRequest: PullRequest = {
    source: "sourceBranch",
    target: "targetBranch",
    title: "modified title",
    destination: "modified description",
    author: "admin",
    id: "1",
    creationDate: "2018-11-28",
    status: "open",
    _links: {}
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should update pull request successfully", done => {
    fetchMock.putOnce("/api/v2" + PULLREQUEST_URL, {
      status: 204
    });

    updatePullRequestComment(PULLREQUEST_URL, pullRequest).then((response: Response) => {
      expect(response.status).toBe(204);
      done();
    });
  });

  it("should fail on creating pull request", done => {
    fetchMock.putOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    updatePullRequestComment(PULLREQUEST_URL, pullRequest).catch((error: Error) => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API create comment", () => {
  const COMMENTS_URL = "/repositories/scmadmin/TestRepo/newPullRequest/comments";

  const comment: BasicComment = {
    comment: "My Comment"
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should create comment successfully", done => {
    fetchMock.postOnce("/api/v2" + COMMENTS_URL, {
      status: 204
    });

    createPullRequestComment(COMMENTS_URL, comment).then(response => {
      expect(response.status).toBe(204);
      done();
    });
  });

  it("should fail on creating comment", done => {
    fetchMock.postOnce("/api/v2" + COMMENTS_URL, {
      status: 500
    });

    createPullRequestComment(COMMENTS_URL, comment).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API update comment", () => {
  const COMMENTS_URL = "/repositories/scmadmin/TestRepo/newPullRequest/comments/1";

  const comment: BasicComment = {
    comment: "My Comment"
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should update comment successfully", done => {
    fetchMock.putOnce("/api/v2" + COMMENTS_URL, {
      status: 204
    });

    updatePullRequestComment(COMMENTS_URL, comment).then(response => {
      expect(response.status).toBe(204);
      done();
    });
  });

  it("should fail on creating comment", done => {
    fetchMock.putOnce("/api/v2" + COMMENTS_URL, {
      status: 500
    });

    updatePullRequestComment(COMMENTS_URL, comment).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API get pull request", () => {
  const PULLREQUEST_URL = "/pull-request/scmadmin/TestRepo/1";

  const pullRequest: PullRequest = {
    source: "sourceBranch",
    target: "targetBranch",
    title: "This is a title",
    author: "admin",
    id: "1",
    creationDate: "2018-11-28",
    status: "open",
    _links: {}
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should fetch pull request successfully", done => {
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, pullRequest);

    getPullRequest(PULLREQUEST_URL).then(response => {
      expect(response).toEqual(pullRequest);
      done();
    });
  });

  it("should fail on fetching pull request", done => {
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    getPullRequest(PULLREQUEST_URL).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API get pull requests", () => {
  const PULLREQUEST_URL = "/pull-requests/scmadmin/TestRepo";

  const pullRequestA: PullRequest = {
    source: "sourceBranchA",
    target: "targetBranchB",
    title: "This is a title A",
    author: "admin",
    id: "1",
    creationDate: "2018-11-28",
    status: "open",
    _links: {}
  };

  const pullRequestB: PullRequest = {
    source: "sourceBranchB",
    target: "targetBranchB",
    title: "This is a title B",
    author: "admin",
    id: "2",
    creationDate: "2018-11-28",
    status: "open",
    _links: {}
  };

  const pullRequests: PullRequest[] = [pullRequestA, pullRequestB];

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should fetch pull requests successfully", done => {
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, pullRequests);

    getPullRequests(PULLREQUEST_URL).then(response => {
      expect(response).toEqual(pullRequests);
      done();
    });
  });

  it("should fail on fetching pull requests", done => {
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    getPullRequests(PULLREQUEST_URL).catch((error: Error) => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API get comments", () => {
  const COMMENTS_URL = "/pull-requests/scmadmin/TestRepo/1/comments";

  const comment_1: Comment = {
    comment: "my 1. comment",
    author: "author",
    date: "2018-12-11T13:55:43.126Z",
    _links: {
      self: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      },
      update: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      },
      delete: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      }
    }
  };
  const comment_2: Comment = {
    comment: "my 2. comment",
    author: "author",
    date: "2018-12-11T13:55:43.126Z",
    _links: {
      self: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      },
      update: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      },
      delete: {
        href: "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/HeartOfGold-git/1/comments/1"
      }
    }
  };

  const comments: Comments = {
    _embedded: {
      pullRequestComments: [comment_1, comment_2]
    }
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should fetch comments successfully", done => {
    fetchMock.getOnce("/api/v2" + COMMENTS_URL, comments);

    getPullRequests(COMMENTS_URL).then(response => {
      expect(response).toEqual(comments);
      done();
    });
  });

  it("should fail on fetching comments", done => {
    fetchMock.getOnce("/api/v2" + COMMENTS_URL, {
      status: 500
    });

    getPullRequests(COMMENTS_URL).catch(error => {
      expect(error).toBeDefined();
      done();
    });
  });
});

describe("API merge pull request", () => {
  const PULLREQUEST_URL = "/repository/scmadmin/TestRepo/merge";

  const pullRequest: PullRequest = {
    source: "sourceBranchA",
    target: "targetBranchB",
    title: "This is a title A",
    author: "admin",
    id: "1",
    creationDate: "2018-11-28",
    status: "open",
    _links: {}
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should merge pull request successfully", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      sourceRevision: pullRequest.source,
      targetRevision: pullRequest.target
    });

    merge(PULLREQUEST_URL, pullRequest).then(() => {
      done();
    });
  });

  it("should fail on fetching pull requests", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    merge(PULLREQUEST_URL, pullRequest).catch((error: Error) => {
      expect(error).toBeDefined();
      done();
    });
  });

  //TODO: test this test again when jenkins uses current version of scmm2
  xit("should return conflict on fetching pull requests", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 409
    });

    merge(PULLREQUEST_URL, pullRequest).then(response => {
      expect(response.conflict).toBeDefined();
      expect(response.error).toBeUndefined();
      done();
    });
  });
});

describe("createChangesetLink tests", () => {
  const baseRepository: Repository = {
    namespace: "hitchhiker",
    name: "deep-thought",
    type: "git"
  };

  const createRepository = (incommingLink: string, templated: boolean) => {
    const links = {};
    if (incommingLink) {
      links.incomingChangesets = {
        href: incommingLink,
        templated
      };
    }

    return {
      ...baseRepository,
      _links: links
    };
  };

  it("should create a valid changeset link", () => {
    const repo = createRepository("/in/{source}/{target}/changesets", true);

    const link = createChangesetUrl(repo, "develop", "master");
    expect(link).toBe("/in/develop/master/changesets");
  });

  it("should return undefined for non templated link", () => {
    const repo = createRepository("/in/{source}/{target}/changesets", false);

    const link = createChangesetUrl(repo, "develop", "master");
    expect(link).toBeUndefined();
  });

  it("should return undefinded for repositories without incomingChangesets link", () => {
    const repo = createRepository();

    const link = createChangesetUrl(repo, "develop", "master");
    expect(link).toBeUndefined();
  });

  it("should encode the branch names", () => {
    const repo = createRepository("/in/{source}/{target}/changesets", true);

    const link = createChangesetUrl(repo, "feature/fjords-of-african", "release/earth-2.0");
    expect(link).toBe("/in/feature%2Ffjords-of-african/release%2Fearth-2.0/changesets");
  });
});
