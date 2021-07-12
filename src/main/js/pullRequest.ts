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

import {
  BasicComment,
  BasicPullRequest,
  Comment,
  Comments,
  Conflicts,
  MergeCommit,
  PossibleTransition,
  PullRequest,
  PullRequestCollection
} from "./types/PullRequest";
import { apiClient } from "@scm-manager/ui-components";
import { Branch, Changeset, HalRepresentation, Link, Links, PagedCollection, Repository } from "@scm-manager/ui-types";
import { useMutation, useQuery, useQueryClient } from "react-query";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

// React-Query Hooks

export const prQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequestId];
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

function getPullRequest(url: string): Promise<PullRequest> {
  return apiClient.get(url).then(response => response.json());
}

export const useUpdatePullRequest = (repository: Repository, pullRequest: PullRequest, callback?: () => void) => {
  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, PullRequest>(
    pr => updatePullRequest(pr).then(callback),
    {
      onSuccess: async pr => {
        queryClient.setQueryData(prQueryKey(repository, pullRequest.id!), pr);
        await queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id!));
      }
    }
  );

  return {
    isLoading,
    error,
    update: (pr: PullRequest) => mutate(pr)
  };
};

function updatePullRequest(pullRequest: PullRequest) {
  return apiClient.put((pullRequest._links.update as Link).href, pullRequest, CONTENT_TYPE_PULLREQUEST);
}

type ApproveRequest = {
  pullRequest: PullRequest;
  shouldApprove: boolean;
};

export const useApproveReviewer = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, ApproveRequest>(
    request => {
      if (request.shouldApprove) {
        return apiClient.post((request.pullRequest._links.approve as Link).href, {});
      } else {
        return apiClient.post((request.pullRequest._links.disapprove as Link).href, {});
      }
    },
    {
      onSuccess: async pr => {
        await queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id!));
      }
    }
  );

  return {
    isLoading,
    error,
    approve: (shouldApprove: boolean) => mutate({ pullRequest, shouldApprove })
  };
};

export const useCreatePullRequest = (repository: Repository, callback?: (id: string) => void) => {
  const queryClient = useQueryClient();
  const { mutate, data, isLoading, error } = useMutation<PullRequest, Error, BasicPullRequest>(
    pr => createPullRequest((repository._links.pullRequest as Link).href, pr, callback),
    {
      onSuccess: pr => {
        queryClient.setQueryData(prQueryKey(repository, pr.id!), pr);
        return queryClient.invalidateQueries(prQueryKey(repository, pr.id!));
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
  const { mutate, isLoading, error } = useMutation<{}, Error, PullRequest>(pr => rejectPullRequest(pr), {
    onSuccess: () => {
      return queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id!));
    }
  });
  return {
    reject: (pr: PullRequest) => mutate(pr),
    isLoading,
    error
  };
};

function rejectPullRequest(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.reject as Link).href, {});
}

type MergeRequest = {
  url: string;
  mergeCommit: MergeCommit;
};

export const useMergePullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, MergeRequest>(
    request => apiClient.post(request.url, request.mergeCommit, "application/vnd.scmm-mergeCommand+json"),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prQueryKey(repository, pullRequest.id!));
      }
    }
  );
  return {
    merge: (request: MergeRequest) => mutate(request),
    isLoading,
    error
  };
};

export const usePullRequests = (repository: Repository, status: string) => {
  const { error, isLoading, data } = useQuery<PullRequestCollection, Error>(
    ["repository", repository.namespace, repository.name, "pull-requests", status],
    () =>
      apiClient
        .get((repository._links.pullRequest as Link).href + "?status=" + status)
        .then(response => response.json())
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useDeletePullRequestComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, Comment>(
    comment => apiClient.delete((comment._links.delete as Link).href),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    deleteComment: (comment: Comment) => mutate(comment),
    isLoading,
    error
  };
};

export const useUpdatePullRequestComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, Comment>(
    comment => apiClient.put((comment._links.update as Link).href, comment),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    updateComment: (comment: Comment) => mutate(comment),
    isLoading,
    error
  };
};

export const useTransformPullRequestComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, PossibleTransition>(
    transition => apiClient.post((transition._links.transform as Link).href, transition),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    transformComment: (transition: PossibleTransition) => mutate(transition),
    isLoading,
    error
  };
};

type CreateCommentRequest = {
  url: string;
  comment: BasicComment;
};

export const useCreatePullRequestComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, CreateCommentRequest>(
    request => apiClient.post(request.url, request.comment),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    createComment: (url: string, comment: BasicComment) => mutate({ url, comment }),
    isLoading,
    error
  };
};

const prCommentsQueryKey = (repository: Repository, pullRequest: PullRequest) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequest.id, "comments"];
};

export const usePullRequestComments = (repository: Repository, pullRequest: PullRequest) => {
  const { error, isLoading, data } = useQuery<Comments, Error>(prCommentsQueryKey(repository, pullRequest), () => {
    if (pullRequest?._links?.comments) {
      return apiClient.get((pullRequest._links.comments as Link).href).then(response => response.json());
    }
    return { _links: {}, _embedded: { pullRequestComments: [] } };
  });

  return {
    error,
    isLoading,
    data
  };
};

export const useSubscription = (repository: Repository, pullRequest: PullRequest) => {
  const { error, isLoading, data } = useQuery<HalRepresentation, Error>(
    [...prQueryKey(repository, pullRequest.id!), "subscription"],
    () => {
      if (pullRequest) {
        return apiClient.get((pullRequest._links.subscription as Link).href).then(response => response.json());
      }
      return { _links: {} };
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

type UpdateSubscriptionRequest = { _links: Links; subscribe: boolean };

export const useUpdateSubscription = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, UpdateSubscriptionRequest>(
    request => {
      if (request.subscribe) {
        return apiClient.post((request._links.subscribe as Link).href);
      }
      return apiClient.post((request._links.unsubscribe as Link).href);
    },
    {
      onSuccess: () => {
        return queryClient.invalidateQueries([...prQueryKey(repository, pullRequest.id!), "subscription"]);
      }
    }
  );
  return {
    subscribe: (_links: Links, subscribe: boolean) => mutate({ _links, subscribe }),
    isLoading,
    error
  };
};

type UpdateReviewMarkRequest = { filePath: string; mark: boolean };

export const useUpdateReviewMark = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, UpdateReviewMarkRequest>(
    request => {
      if (request.mark) {
        return apiClient.post((pullRequest._links.reviewMark as Link).href.replace("{path}", request.filePath), {});
      } else {
        return apiClient.delete((pullRequest._links.reviewMark as Link).href.replace("{path}", request.filePath));
      }
    },
    {
      onSuccess: () => {
        return queryClient.invalidateQueries([...prQueryKey(repository, pullRequest.id!)]);
      }
    }
  );
  return {
    mark: (filePath: string, mark: boolean) => mutate({ filePath, mark }),
    isLoading,
    error
  };
};

type ChangesetCollection = PagedCollection & {
  _embedded: {
    changesets: Changeset[];
  };
};

export const usePullRequestChangesets = (repository: Repository, pullRequest: PullRequest, page: number) => {
  const url = createChangesetUrl(repository, pullRequest.source, pullRequest.target) + `?page=${page - 1}`;
  const { error, isLoading, data } = useQuery<ChangesetCollection, Error>(
    [...prQueryKey(repository, pullRequest?.id || pullRequest.source + pullRequest.target), "changesets"],
    () => {
      return apiClient.get(url).then(r => r.json());
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

export const usePullRequestConflicts = (repository: Repository, pullRequest: PullRequest) => {
  const { error, isLoading, data } = useQuery<Conflicts, Error>(
    [...prQueryKey(repository, pullRequest.id!), "conflicts"],
    () => {
      return apiClient
        .post(
          (pullRequest._links.mergeConflicts as Link).href,
          { sourceRevision: pullRequest.source, targetRevision: pullRequest.target },
          "application/vnd.scmm-mergeCommand+json"
        )
        .then(r => r.json());
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

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

export function checkPullRequest(url: string, pullRequest: BasicPullRequest) {
  const checkUrl = url + `?source=${pullRequest.source}&target=${pullRequest.target}`;
  return apiClient.get(checkUrl);
}

export function check(pullRequest: PullRequest) {
  return apiClient.post((pullRequest._links.mergeCheck as Link).href, {}).then(response => response.json());
}

export function getMergeStrategyInfo(url: string) {
  return apiClient.get(url).then(response => response.json());
}

function createIncomingUrl(repository: Repository, linkName: string, source: string, target: string) {
  const link = repository._links[linkName];
  if ((link as Link)?.templated) {
    return (link as Link).href
      .replace("{source}", encodeURIComponent(source))
      .replace("{target}", encodeURIComponent(target));
  }
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

export function evaluateTagColor(pullRequest: PullRequest) {
  if (pullRequest.status === "MERGED") {
    return "success";
  } else if (pullRequest.status === "REJECTED") {
    return "danger";
  }
  return "light";
}
