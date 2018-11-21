// @flow
/*import fetchMock from "fetch-mock";
import type { PullRequest } from "./PullRequest";
import { createPullRequest, getBranches } from "./pullRequest";

xdescribe("API create pull request", () => {
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
*/
