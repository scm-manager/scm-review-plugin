// @flow
import fetchMock from "fetch-mock";
import type { PullRequest } from "./PullRequest";
import {
  createPullRequest,
  createPullRequestComment,
  getBranches,
  getPullRequest,
  getPullRequestComments,
  getPullRequests
} from "./pullRequest";
import {Comment} from "./types/PullRequest";
import type {Comments} from "./types/PullRequest";

describe("API create pull request", () => {
  const PULLREQUEST_URL = "/repositories/scmadmin/TestRepo/newPullRequest";

  const BRANCH_URL = "/repositories/scmadmin/TestRepo/branches";

  const pullRequest: PullRequest = {
    source: "sourceBranch",
    target: "targetBranch",
    title: "This is a title"
  };

  const branchRequest = {
    _embedded: {
      branches: [
        {
          name: "branchA"
        },
        {
          name: "branchB"
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
      status: 204
    });

    createPullRequest(PULLREQUEST_URL, pullRequest).then(response => {
      expect(response.status).toBe(204);
      expect(response.error).toBeUndefined();
      done();
    });
  });

  it("should fail on creating pull request", done => {
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    createPullRequest(PULLREQUEST_URL, pullRequest).then(response => {
      expect(response.error).toBeDefined();
      done();
    });
  });

  it("should get branches successfully", done => {
    fetchMock.getOnce("/api/v2" + BRANCH_URL, branchRequest);

    getBranches(BRANCH_URL).then(response => {
      expect(response).toEqual(["branchA", "branchB"]);
      expect(response.error).toBeUndefined();
      done();
    });
  });

  it("should fail on getting branches", done => {
    fetchMock.getOnce("/api/v2" + BRANCH_URL, {
      status: 500
    });

    getBranches(BRANCH_URL).then(response => {
      expect(response.error).toBeDefined();
      done();
    });
  });

});


describe("API create comment", () => {
  const COMMENTS_URL = "/repositories/scmadmin/TestRepo/newPullRequest/comments";

  const comment: Comment = {
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
      expect(response.error).toBeUndefined();
      done();
    });
  });

  it("should fail on creating comment", done => {
    fetchMock.postOnce("/api/v2" + COMMENTS_URL, {
      status: 500
    });

    createPullRequest(COMMENTS_URL, comment).then(response => {
      expect(response.error).toBeDefined();
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
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL,
      pullRequest);

    getPullRequest(PULLREQUEST_URL)
      .then(response => {
        expect(response).toEqual(pullRequest);
        done();
      });
  });

  it("should fail on fetching pull request", done => {

    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    getPullRequest(PULLREQUEST_URL)
      .then(response => {
        expect(response.error).toBeDefined();
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

  const pullRequests: PullRequest[] = [
    pullRequestA,
    pullRequestB
  ];

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should fetch pull requests successfully", done => {
    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL,
      pullRequests);

    getPullRequests(PULLREQUEST_URL)
      .then(response => {
        expect(response).toEqual(pullRequests);
        done();
      });
  });

  it("should fail on fetching pull requests", done => {

    fetchMock.getOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    getPullRequests(PULLREQUEST_URL)
      .then(response => {
        expect(response.error).toBeDefined();
        done();
      });
  });
});

describe("API get comments", () => {

  const COMMENTS_URL = "/pull-requests/scmadmin/TestRepo/1/comments";

  const comment_1: Comment= {
    comment : "my 1. comment"
  };

  const comment_2: Comment= {
    comment : "my 2. comment"
  };

  const comments: Comments ={
    _embedded: {
      pullRequestComments: [
        comment_1,
        comment_2
      ]
    }
  };

  afterEach(() => {
    fetchMock.reset();
    fetchMock.restore();
  });

  it("should fetch comments successfully", done => {
    fetchMock.getOnce("/api/v2" + COMMENTS_URL,
      comments);

    getPullRequests(COMMENTS_URL)
      .then(response => {
        expect(response).toEqual(comments);
        done();
      });
  });

  it("should fail on fetching comments", done => {

    fetchMock.getOnce("/api/v2" + COMMENTS_URL, {
      status: 500
    });

    getPullRequests(COMMENTS_URL)
      .then(response => {
        expect(response.error).toBeDefined();
        done();
      });
  });
});

