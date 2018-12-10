//@flow
import type { PullRequest } from "./types/PullRequest";
import { apiClient, CONFLICT_ERROR } from "@scm-manager/ui-components";

export function createPullRequest(url: string, pullRequest: PullRequest) {
  return apiClient
    .post(url, pullRequest)
    .then(response => {
      return response;
    })
    .catch(cause => {
      const error = new Error(
        `could not create pull request: ${cause.message}`
      );
      return { error: error };
    });
};

export function getBranches(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(collection => collection._embedded.branches)
    .then(branches => {
      return branches.map(b => b.name);
    })
    .catch(cause => {
      const error = new Error(`could not fetch branches: ${cause.message}`);
      return { error: error };
    });
};

export function getPullRequest(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequest => {
      return pullRequest;
    })
    .catch(cause => {
      const error = new Error(`could not fetch pull request: ${cause.message}`);
      return {error: error};
    });
};

export function getPullRequests(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequests => {
      return pullRequests;
    })
    .catch(cause => {
      const error = new Error(`could not fetch pull requests: ${cause.message}`);
      return {error: error};
    });
};

export function merge(url: string, pullRequest: PullRequest){
  return apiClient
    .post(url, {
      sourceRevision: pullRequest.source,
      targetRevision: pullRequest.target
    }, "application/vnd.scmm-mergeCommand+json")
    .catch(cause => {
      if(cause === CONFLICT_ERROR){
        return {conflict: cause};
      }
      else {
        const error = new Error(`could not merge pull request: ${cause.message}`);
        return {error: error};
      }
    });
};
