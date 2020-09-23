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

import { BasicComment, BasicPullRequest, MergeCommit, PossibleTransition, PullRequest } from "./types/PullRequest";
import { apiClient } from "@scm-manager/ui-components";
import { Branch, Link, Repository } from "@scm-manager/ui-types";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

export function createPullRequest(url: string, pullRequest: BasicPullRequest) {
  return apiClient
    .post(url, pullRequest, CONTENT_TYPE_PULLREQUEST)
    .then(response => {
      const location = response.headers.get("Location");
      if (!location) {
        throw new Error("missing location header in response from create request");
      }
      return apiClient.get(location + "?fields=id");
    })
    .then(response => response.json())
    .then(pr => pr.id);
}

export function updatePullRequest(url: string, pullRequest: PullRequest) {
  return apiClient.put(url, pullRequest, CONTENT_TYPE_PULLREQUEST);
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
      const branchNames = branches.map((b: Branch) => b.name);
      const defaultBranch = branches.find((b: Branch) => b.defaultBranch);
      return {
        branchNames,
        defaultBranch
      };
    });
}

export function getPullRequest(url: string): Promise<PullRequest> {
  return apiClient.get(url).then(response => response.json());
}

export function getPullRequests(url: string) {
  return apiClient.get(url).then(response => response.json());
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

export function check(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.mergeCheck as Link).href, {}).then(response => response.json());
}

export function getMergeStrategyInfo(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => "");
}

export function getChangesets(url: string) {
  return apiClient.get(url).then(response => response.json());
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
  if (repository._links.incomingDiffParsed) {
    return createIncomingUrl(repository, "incomingDiffParsed", source, target);
  } else {
    return createIncomingUrl(repository, "incomingDiff", source, target);
  }
}

export function postReviewMark(url: string, path: string) {
  return apiClient.post(url.replace("{path}", path), {});
}

export function deleteReviewMark(url: string, path: string) {
  return apiClient.delete(url.replace("{path}", path));
}

function createIncomingUrl(repository: Repository, linkName: string, source: string, target: string) {
  const link = repository._links[linkName];
  if (link && (link as Link).templated) {
    return (link as Link).href
      .replace("{source}", encodeURIComponent(source))
      .replace("{target}", encodeURIComponent(target));
  }
}

export function reject(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.reject as Link).href, {});
}

export function fetchConflicts(url: string, source: string, target: string) {
  return apiClient
    .post(url, { sourceRevision: source, targetRevision: target }, "application/vnd.scmm-mergeCommand+json")
    .then(response => response.json())
    .catch(err => {
      return {
        error: err
      };
    });
}

export function evaluateTagColor(pullRequest: PullRequest) {
  if (pullRequest.status === "MERGED") {
    return "success";
  } else if (pullRequest.status === "REJECTED") {
    return "danger";
  }
  return "light";
}
