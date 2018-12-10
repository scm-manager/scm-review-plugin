// @flow
import fetchMock from "fetch-mock";
import type { PullRequest } from "./PullRequest";
import {createPullRequest, getBranches, getPullRequest, getPullRequests, merge} from "./pullRequest";

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
    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL,
      {
        sourceRevision: pullRequest.source,
        targetRevision: pullRequest.target
      }
      );

    merge(PULLREQUEST_URL, pullRequest)
      .then(response => {
        expect(response.error).toBeUndefined();
        done();
      });
  });

  it("should fail on fetching pull requests", done => {

    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 500
    });

    merge(PULLREQUEST_URL, pullRequest)
      .then(response => {
        expect(response.error).toBeDefined();
        done();
      });
  });

  it("should return conflict on fetching pull requests", done => {

    fetchMock.postOnce("/api/v2" + PULLREQUEST_URL, {
      status: 409
    });

    merge(PULLREQUEST_URL, pullRequest)
      .then(response => {
        expect(response.conflict).toBeDefined();
        expect(response.error).toBeUndefined();
        done();
      });
  });
});
