import { BasicComment, BasicPullRequest, MergeCommit, PossibleTransition, PullRequest } from "./types/PullRequest";
import { apiClient, ConflictError, NotFoundError } from "@scm-manager/ui-components";
import { Link, Repository } from "@scm-manager/ui-types";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

export function createPullRequest(url: string, pullRequest: BasicPullRequest) {
  return apiClient
    .post(url, pullRequest, CONTENT_TYPE_PULLREQUEST)
    .then(response => {
      return response;
    })
    .catch(err => {
      return {
        error: err
      };
    });
}

export function updatePullRequest(url: string, pullRequest: PullRequest) {
  return apiClient
    .put(url, pullRequest, CONTENT_TYPE_PULLREQUEST)
    .then(response => {
      return response;
    })
    .catch(err => {
      return {
        error: err
      };
    });
}

export function createPullRequestComment(url: string, comment: BasicComment) {
  return apiClient.post(url, comment).then(response => {
    return response;
  });
}

export function updatePullRequestComment(url: string, comment: BasicComment) {
  return apiClient.put(url, comment);
}

export function transformPullRequestComment(transition: PossibleTransition) {
  const link = transition._links.transform as Link;
  return apiClient.post(link.href, transition);
}

export function getBranches(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(collection => collection._embedded.branches)
    .then(branches => {
      const branchNames = branches.map(b => b.name);
      const defaultBranch = branches.find(b => b.defaultBranch);
      return {
        branchNames,
        defaultBranch
      };
    })
    .catch(err => {
      return {
        error: err
      };
    });
}

export function getPullRequest(url: string): Promise<PullRequest> {
  return apiClient
    .get(url)
    .then(response => response.json());
}

export function getPullRequests(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(pullRequests => {
      return pullRequests;
    })
    .catch(err => {
      return {
        error: err
      };
    });
}

export function getReviewer(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(reviewer => {
      return reviewer;
    })
    .catch(err => {
      return {
        error: err
      };
    });
}

export function getApproval(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => {
      return {
        error: err
      };
    });
}

export function handleApproval(url: string) {
  return apiClient.post(url, {});
}

export function getSubscription(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => {
      return {
        error: err
      };
    });
}

export function handleSubscription(url: string) {
  return apiClient.post(url).catch((err: Error) => {
    return {
      error: err
    };
  });
}

export function merge(url: string, mergeCommit: MergeCommit) {
  return apiClient.post(url, mergeCommit, "application/vnd.scmm-mergeCommand+json");
}

export function dryRun(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.mergeDryRun as Link).href, {})
}

export function getDefaultCommitDefaultMessage(url: string) {
  return apiClient
    .get(url)
    .then(response => response.text())
    .catch(err => "");
}

export function getChangesets(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => {
      return {
        error: err
      };
    });
}

export function getPullRequestComments(url: string) {
  return apiClient.get(url).then(response => response.json());
}

export function deletePullRequestComment(url: string) {
  return apiClient.delete(url);
}

export function createChangesetUrl(repository: Repository, source: string, target: string) {
  return createIncomingUrl(repository, "incomingChangesets", source, target);
}

export function createDiffUrl(repository: Repository, source: string, target: string) {
  return createIncomingUrl(repository, "incomingDiff", source, target);
}

function createIncomingUrl(repository: Repository, linkName: string, source: string, target: string) {
  const link = repository._links[linkName];
  if (link && link.templated) {
    return link.href.replace("{source}", encodeURIComponent(source)).replace("{target}", encodeURIComponent(target));
  }
}

export function reject(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.reject as Link).href, {});
}
