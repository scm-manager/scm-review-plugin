import type { PullRequest } from "./types/PullRequest";
import { apiClient } from "@scm-manager/ui-components";

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
}

export function createPullRequestComment(url: string, comment: Comment) {
  return apiClient
    .post(url, comment)
    .then(response => {
      return response;
    })
    .catch(cause => {
      const error = new Error(
        `could not create pull request comment: ${cause.message}`
      );
      return { error: error };
    });
}

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
}

export function getPullRequest(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequest => {
      return pullRequest
    })
    .catch(cause => {
      const error = new Error(`could not fetch pull request: ${cause.message}`);
      return {error: error};
    })
}

export function getPullRequests(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequests => {
      return pullRequests
    })
    .catch(cause => {
      const error = new Error(`could not fetch pull requests: ${cause.message}`);
      return {error: error};
    })
}

export function getPullRequestComments(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequestComments => {
      return pullRequestComments
    })
    .catch(cause => {
      const error = new Error(`could not fetch pull request comments: ${cause.message}`);
      return {error: error};
    })
}

export function deletePullRequestComment(url: string){
  return apiClient
    .delete(url)
    .then(response => {
      return response;
    })
    .catch(cause => {
      const error = new Error(`could not delete pull request comments: ${cause.message}`);
      return {error: error};
    })
}
