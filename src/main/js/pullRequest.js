//@flow
import type {PullRequest} from "./types/PullRequest";
import {apiClient, CONFLICT_ERROR} from "@scm-manager/ui-components";

export function createPullRequest(url: string, pullRequest: PullRequest) {
  return apiClient
    .post(url, pullRequest)
    .then(response => {
      return response;
    })
    .catch(err => {
      return { error: err };
    });
};

export function createPullRequestComment(url: string, comment: Comment) {
  return apiClient
    .post(url, comment)
    .then(response => {
      return response;
    })
    .catch(err => {
      return { error: err };
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
    .catch(err => {
      return { error: err };
    });
};

export function getPullRequest(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequest => {
      return pullRequest;
    })
    .catch(err => {
      return {error: err};
    });
};

export function getPullRequests(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequests => {
      return pullRequests;
    })
    .catch(err => {
      return {error: err};
    });
};

export function merge(url: string, pullRequest: PullRequest){
  return apiClient
    .post(url, {
      sourceRevision: pullRequest.source,
      targetRevision: pullRequest.target
    }, "application/vnd.scmm-mergeCommand+json")
    .catch(err => {
      if(err === CONFLICT_ERROR){
        return {conflict: cause};
      }
      else {
        return {error: err};
      }
    });
};

export function getChangesets(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => {
      return {error: err};
    });
};

export function getPullRequestComments(url: string){
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequestComments => {
      return pullRequestComments;
    })
    .catch(err => {
      return {error: err};
    });
};

export function deletePullRequestComment(url: string){
  return apiClient
    .delete(url)
    .then(response => {
      return response;
    })
    .catch(err => {
      return {error: err};
    })
};

export function createChangesetUrl(repository: Repository, source: string, target: string) {
  const link = repository._links.incomingChangesets;
  if (link && link.templated) {
    return link.href.replace("{source}", encodeURIComponent(source)).replace("{target}", encodeURIComponent(target));
  }
}

export function reject(pullRequest: PullRequest){
  return apiClient
    .post(pullRequest._links.reject.href);
}
