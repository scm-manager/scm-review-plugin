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
import { useMutation, useQuery, useQueryClient } from "react-query";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

// React-Query Hooks

const prQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequestId];
};

export const invalidatePullRequest = async (repository: Repository, pullRequestId: string) => {
  const queryClient = useQueryClient();
  await queryClient.invalidateQueries(prQueryKey(repository, pullRequestId));
};

export const usePullRequest = (repository: Repository, pullRequestId: string) => {
  const { error, isLoading, data } = useQuery<PullRequest, Error>(prQueryKey(repository, pullRequestId), () =>
    getPullRequest((repository._links.pullRequest as Link).href + "/" + pullRequestId)
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useUpdatePullRequest = (repository: Repository, pullRequest: PullRequest, callback?: () => void) => {
  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, PullRequest>(
    pr => updatePullRequest(pr).then(callback),
    {
      onSuccess: async pr => {
        queryClient.setQueryData(prQueryKey(repository, pullRequest.id), pr);
        await queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id));
      }
    }
  );

  return {
    isLoading,
    error,
    update: (pr: PullRequest) => mutate(pr)
  };
};

export function updatePullRequest(pullRequest: PullRequest) {
  return apiClient.put((pullRequest._links.update as Link).href, pullRequest, CONTENT_TYPE_PULLREQUEST);
}

export const useApproveReviewer = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, PullRequest>(pr => handleApproval(pr), {
    onSuccess: async pr => {
      queryClient.setQueryData(prQueryKey(repository, pullRequest.id), pr);
      await queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id));
    }
  });

  return {
    isLoading,
    error,
    approve: () => mutate(pullRequest)
  };
};

export const useDisapproveReviewer = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, PullRequest>(pr => handleDisapproval(pr), {
    onSuccess: async pr => {
      queryClient.setQueryData(prQueryKey(repository, pullRequest.id), pr);
      await queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id));
    }
  });

  return {
    isLoading,
    error,
    disapprove: () => mutate(pullRequest)
  };
};


export function handleApproval(pr: PullRequest) {
  return apiClient.post((pr._links.approve as Link).href, {});
}

export function handleDisapproval(pr: PullRequest) {
  return apiClient.post((pr._links.disapprove as Link).href, {});
}

export const useCreatePullRequest = (repository: Repository, callback?: (id: string) => void) => {
  const queryClient = useQueryClient();
  const { mutate, data, isLoading, error } = useMutation<PullRequest, Error, BasicPullRequest>(
    pr => createPullRequest((repository._links.pullRequest as Link).href, pr, callback),
    {
      onSuccess: pr => {
        queryClient.setQueryData(prQueryKey(repository, pr.id), pr);
        return queryClient.invalidateQueries(prQueryKey(repository, pr.id));
      }
    }
  );
  return {
    create: (pr: BasicPullRequest) => mutate(pr),
    isLoading,
    error,
    data
  };
};

export function createPullRequest(url: string, pullRequest: BasicPullRequest, callback?: (id: string) => void) {
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
    .then(pr => {
      if (callback) {
        callback(pr.id);
      }
      return pr;
    });
}


export const useRejectPullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, PullRequest>(
    pr => rejectPullRequest(pr),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id));
      }
    }
  );
  return {
    reject: (pr: PullRequest) => mutate(pr),
    isLoading,
    error,
  };
};

export function rejectPullRequest(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.reject as Link).href, {});
}

type MergeRequest = {
  url: string;
  mergeCommit: MergeCommit;
}

export const useMergePullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, MergeRequest>(
    request => merge(request),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id));
      }
    }
  );
  return {
    merge: (request: MergeRequest) => mutate(request),
    isLoading,
    error,
  };
};

export function merge(request: MergeRequest) {
  return apiClient.post(request.url, request.mergeCommit, "application/vnd.scmm-mergeCommand+json");
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


export function check(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.mergeCheck as Link).href, {}).then(response => response.json());
}

export function getMergeStrategyInfo(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .catch(err => "");
}

export function checkPullRequest(url: string, pullRequest: BasicPullRequest) {
  const checkUrl = url + `?source=${pullRequest.source}&target=${pullRequest.target}`;
  return apiClient.get(checkUrl);
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
  if ((link as Link)?.templated) {
    return (link as Link).href
      .replace("{source}", encodeURIComponent(source))
      .replace("{target}", encodeURIComponent(target));
  }
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
