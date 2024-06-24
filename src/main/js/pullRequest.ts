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
  CheckResult,
  Comment,
  CommentImage,
  Comments,
  Conflicts,
  MergeCheck,
  MergeCommit,
  PagedPullRequestCollection,
  PossibleTransition,
  PullRequest
} from "./types/PullRequest";
import { apiClient, ConflictError, NotFoundError } from "@scm-manager/ui-components";
import { Changeset, HalRepresentation, Link, PagedCollection, Repository } from "@scm-manager/ui-types";
import { QueryClient, useMutation, useQuery, useQueryClient } from "react-query";
import { useBranches } from "@scm-manager/ui-api";
import { PullRequestChange } from "./change/ChangesTypes";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

// React-Query Hooks

export const useInvalidatePullRequest = (repository: Repository, pullRequest: PullRequest, inclusiveDiff?: boolean) => {
  const queryClient = useQueryClient();
  const pullRequestId = pullRequest.id;
  if (!pullRequestId) {
    throw new Error("pull request with found");
  }

  let diffUrl: string;
  if (inclusiveDiff && pullRequest.source && pullRequest.target) {
    diffUrl = createDiffUrl(repository, pullRequest.source, pullRequest.target);
  }

  return () => {
    const invalidations = [
      queryClient.invalidateQueries(prQueryKey(repository, pullRequestId)),
      queryClient.invalidateQueries(["mergeCheck", ...prQueryKey(repository, pullRequestId)])
    ];

    if (diffUrl) {
      invalidations.push(queryClient.invalidateQueries(["link", diffUrl]));
    }
    return Promise.all(invalidations);
  };
};

export const useInvalidateDiff = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const diffUrl = createDiffUrl(repository, pullRequest.source, pullRequest.target);
  return async () => {
    if (diffUrl) {
      await queryClient.invalidateQueries(["link", diffUrl]);
    }
  };
};

export const prQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequestId];
};

export const prsQueryKey = (repository: Repository, status?: string) => {
  return ["repository", repository.namespace, repository.name, "pull-requests", status || ""];
};

const prCommentsQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequestId, "comments"];
};

const prMergeCheckQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["merge-check", ...prQueryKey(repository, pullRequestId)];
};

export const invalidateQueries = (queryClient: QueryClient, ...keys: string[][]): Promise<void> => {
  return Promise.all(keys.map(key => queryClient.invalidateQueries(key))).then(() => undefined);
};

export const usePullRequest = (repository: Repository, pullRequestId?: string) => {
  if (!pullRequestId) {
    throw new Error("Could not fetch pull request without id");
  }

  const { error, isLoading, data } = useQuery<PullRequest, Error>(prQueryKey(repository, pullRequestId), () =>
    apiClient.get(requiredLink(repository, "pullRequest") + "/" + pullRequestId).then(response => response.json())
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useUpdatePullRequest = (repository: Repository, pullRequest: PullRequest, callback?: () => void) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not update pull request without id");
  }

  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, PullRequest>(
    pr => {
      return apiClient.put(requiredLink(pr, "update"), pr, CONTENT_TYPE_PULLREQUEST);
    },
    {
      onSuccess: () => {
        if (callback) {
          callback();
        }
        return queryClient.invalidateQueries(prQueryKey(repository, id));
      }
    }
  );

  return {
    isLoading,
    error,
    update: (pr: PullRequest) => mutate(pr)
  };
};

export const useApproveReviewer = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not approve pull request without id");
  }

  const queryClient = useQueryClient();
  const { isLoading, error, mutate } = useMutation<unknown, Error, string>((link: string) => apiClient.post(link, {}), {
    onSuccess: () => {
      return invalidateQueries(queryClient, prQueryKey(repository, id), prMergeCheckQueryKey(repository, id));
    }
  });

  const approve = () => mutate((pullRequest._links.approve as Link).href);
  const disapprove = () => mutate((pullRequest._links.disapprove as Link).href);

  return {
    isLoading,
    error,
    approve: pullRequest._links.approve ? approve : undefined,
    disapprove: pullRequest._links.disapprove ? disapprove : undefined
  };
};

export const useCreatePullRequest = (repository: Repository, callback?: (id: string) => void) => {
  const queryClient = useQueryClient();
  const { mutate, data, isLoading, error } = useMutation<PullRequest, Error, BasicPullRequest>(
    pr => {
      return createPullRequest(requiredLink(repository, "pullRequest"), pr);
    },
    {
      onSuccess: pr => {
        const id = pr.id;
        if (!id) {
          throw new Error("created pull request missing id");
        }
        if (callback) {
          callback(id);
        }
        queryClient.setQueryData(prQueryKey(repository, id), pr);
        return invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, id));
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

function createPullRequest(url: string, pullRequest: BasicPullRequest) {
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
    .then(pr => pr);
}

export const useRejectPullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not reject pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, string>(
    message => {
      const rejectLink = requiredLink(pullRequest, "rejectWithMessage");
      return apiClient.post(rejectLink, { message });
    },
    {
      onSuccess: () => {
        return invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, id));
      }
    }
  );
  return {
    reject: (message: string) => mutate(message),
    isLoading,
    error
  };
};

export const useReopenPullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not reopen pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutateAsync, isLoading, error } = useMutation<unknown, Error>(
    () => apiClient.post(requiredLink(pullRequest, "reopen")),
    {
      onSuccess: () => invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, id))
    }
  );
  return {
    reopen: () => mutateAsync(),
    isLoading,
    error
  };
};

type MergeRequest = {
  url: string;
  mergeCommit: MergeCommit;
};

export const useMergePullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not merge pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, MergeRequest>(
    request => {
      if (!request.url) {
        throw new Error("Could not merge because merge url is not defined");
      }
      return apiClient.post(request.url, request.mergeCommit, "application/vnd.scmm-mergeCommand+json");
    },
    {
      onSuccess: () => {
        return invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, id));
      }
    }
  );
  return {
    merge: (request: MergeRequest) => mutate(request),
    isLoading,
    error
  };
};

export const useReadyForReviewPullRequest = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not modify pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, PullRequest>(
    pr => {
      return apiClient.post(requiredLink(pr, "convertToPR"), {});
    },
    {
      onSuccess: () => {
        return invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, id));
      }
    }
  );
  return {
    readyForReview: (pr: PullRequest) => mutate(pr),
    isLoading,
    error
  };
};

type UsePullRequestsRequest = {
  status?: string;
  sortBy?: string;
  page?: number;
  pageSize?: number;
};

export const usePullRequests = (repository: Repository, request?: UsePullRequestsRequest) => {
  const status = request?.status || "IN_PROGRESS";
  const sortBy = request?.sortBy || "LAST_MOD_DESC";
  const page = request?.page ? request.page - 1 : 0;
  const pageSize = request?.pageSize || 10;
  const { error, isLoading, data } = useQuery<PagedPullRequestCollection, Error>(
    ["repository", repository.namespace, repository.name, "pull-requests", status, sortBy, page],
    () => {
      const link =
        requiredLink(repository, "pullRequest") +
        `?status=${status}&sortBy=${sortBy}&page=${page}&pageSize=${pageSize}`;
      return apiClient.get(link).then(response => response.json());
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useDeleteComment = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not delete comment for pull request without id");
  }
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, Comment>(
    comment => apiClient.delete(requiredLink(comment, "delete")),
    {
      onSuccess: () => {
        return invalidateQueries(
          queryClient,
          prQueryKey(repository, id),
          prCommentsQueryKey(repository, id),
          prMergeCheckQueryKey(repository, id)
        );
      }
    }
  );
  return {
    remove: (comment: Comment) => mutate(comment),
    isLoading,
    error
  };
};

export const useUpdateComment = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not update comment fpr pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, Comment>(
    comment => apiClient.put(requiredLink(comment, "update"), comment),
    {
      onSuccess: () => {
        return invalidateQueries(queryClient, prQueryKey(repository, id), prCommentsQueryKey(repository, id));
      }
    }
  );
  return {
    update: (comment: Comment) => mutate(comment),
    isLoading,
    error
  };
};

const extractCommentWithFileTypes = (request: CreateCommentWithImagesRequest | UpdateCommentWithImagesRequest) => {
  const filetypes: { [key: string]: string } = {};
  request.images.forEach(image => (filetypes[image.fileHash] = image.filetype));
  return { ...request.comment, filetypes };
};

export const useUpdateCommentWithImages = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not update comment fpr pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, UpdateCommentWithImagesRequest>(
    updateRequest => {
      const formData = new FormData();
      updateRequest.images.forEach(image => formData.append(image.fileHash, image.file, image.fileHash));
      formData.append("comment", JSON.stringify(extractCommentWithFileTypes(updateRequest)));

      const options: RequestInit = {
        method: "PUT",
        body: formData
      };

      return apiClient.httpRequestWithBinaryBody(options, requiredLink(updateRequest.comment, "updateWithImages"));
    },
    {
      onSuccess: () => {
        return invalidateQueries(queryClient, prQueryKey(repository, id), prCommentsQueryKey(repository, id));
      }
    }
  );
  return {
    update: (comment: Comment, images: CommentImage[]) => mutate({ comment, images }),
    isLoading,
    error
  };
};

export const useTransformComment = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not transform comment for pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, PossibleTransition>(
    transition => apiClient.post(requiredLink(transition, "transform"), transition),
    {
      onSuccess: () => {
        return invalidateQueries(
          queryClient,
          prQueryKey(repository, id),
          prCommentsQueryKey(repository, id),
          prMergeCheckQueryKey(repository, id)
        );
      }
    }
  );
  return {
    transform: (transition: PossibleTransition) => mutate(transition),
    isLoading,
    error
  };
};

type CreateCommentRequest = {
  url: string;
  comment: BasicComment;
};

type CreateCommentWithImagesRequest = CreateCommentRequest & {
  images: CommentImage[];
};

type UpdateCommentWithImagesRequest = {
  comment: Comment;
  images: CommentImage[];
};

export const useCreateComment = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not create comment for pull request without id");
  }
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, CreateCommentRequest>(
    request => {
      if (!request.url) {
        throw new Error("Could not create comment because create url is not defined");
      }
      return apiClient.post(request.url, request.comment);
    },
    {
      onSuccess: () => {
        return invalidateQueries(
          queryClient,
          prQueryKey(repository, id),
          prCommentsQueryKey(repository, id),
          prMergeCheckQueryKey(repository, id)
        );
      }
    }
  );
  return {
    create: (url: string, comment: BasicComment) => mutate({ url, comment }),
    isLoading,
    error
  };
};

export const useCreateCommentWithImage = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not create comment for pull request without id");
  }
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, CreateCommentWithImagesRequest>(
    request => {
      if (!request.url) {
        throw new Error("Could not create comment because create url is not defined");
      }

      return apiClient.postBinary(request.url, formdata => {
        request.images.forEach(image => formdata.append(image.fileHash, image.file, image.fileHash));
        formdata.append("comment", JSON.stringify(extractCommentWithFileTypes(request)));
      });
    },
    {
      onSuccess: () => {
        return invalidateQueries(
          queryClient,
          prQueryKey(repository, id),
          prCommentsQueryKey(repository, id),
          prMergeCheckQueryKey(repository, id)
        );
      }
    }
  );
  return {
    create: (url: string, comment: BasicComment, images: CommentImage[]) => mutate({ url, comment, images }),
    isLoading,
    error
  };
};

export const useComments = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  const { error, isLoading, data } = useQuery<Comments, Error>(
    prCommentsQueryKey(repository, id || "new_pull_request"),
    () => {
      if (pullRequest?._links?.comments) {
        return apiClient.get((pullRequest._links.comments as Link).href).then(response => response.json());
      }
      return { _links: {}, _embedded: { pullRequestComments: [] } };
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useSubscription = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;
  if (!id) {
    throw new Error("Could not read subscription for pull request without id");
  }
  const { error, isLoading, data } = useQuery<HalRepresentation, Error>(
    [...prQueryKey(repository, id), "subscription"],
    () => {
      if (pullRequest._links.subscription) {
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

export const useUpdateSubscription = (repository: Repository, pullRequest: PullRequest, data: HalRepresentation) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not subscribe to pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate: subscribe, isLoading: subscribeLoading, error: subscribeError } = useMutation<unknown, Error, void>(
    () => apiClient.post((data._links.subscribe as Link).href),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries([...prQueryKey(repository, id), "subscription"]);
      }
    }
  );

  const { mutate: unsubscribe, isLoading: unsubscribeLoading, error: unsubscribeError } = useMutation<
    unknown,
    Error,
    void
  >(() => apiClient.post((data._links.unsubscribe as Link).href), {
    onSuccess: () => {
      return queryClient.invalidateQueries([...prQueryKey(repository, id), "subscription"]);
    }
  });

  return {
    subscribe: data._links.subscribe ? () => subscribe() : undefined,
    unsubscribe: data._links.unsubscribe ? () => unsubscribe() : undefined,
    isLoading: subscribeLoading || unsubscribeLoading,
    error: unsubscribeError || subscribeError
  };
};

export const useUpdateReviewMark = (repository: Repository, pullRequest: PullRequest, filePath: string) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not mark files in pull request without id");
  }

  const queryClient = useQueryClient();
  const { mutate: mark, isLoading: markLoading, error: markError } = useMutation<unknown, Error, void>(
    () => apiClient.post((pullRequest._links.reviewMark as Link).href.replace("{path}", filePath), {}),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries([...prQueryKey(repository, id)]);
      }
    }
  );

  const { mutate: unmark, isLoading: unmarkLoading, error: unmarkError } = useMutation<unknown, Error, void>(
    () => apiClient.delete((pullRequest._links.reviewMark as Link).href.replace("{path}", filePath)),
    {
      onSuccess: () => {
        return queryClient.invalidateQueries([...prQueryKey(repository, id)]);
      }
    }
  );

  return {
    mark: () => mark(),
    unmark: () => unmark(),
    isLoading: markLoading || unmarkLoading,
    error: markError || unmarkError
  };
};

export const usePullRequestChanges = (repository: Repository, pullRequest: PullRequest) => {
  const { error, isLoading, data } = useQuery<PullRequestChange[], Error>(
    [...prQueryKey(repository, pullRequest?.id || pullRequest.source + pullRequest.target), "changes"],
    async () => {
      const url = requiredLink(pullRequest, "changes");
      return (await apiClient.get(url)).json();
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

type ChangesetCollection = PagedCollection & {
  _embedded: {
    changesets: Changeset[];
  };
};

export const usePullRequestChangesets = (repository: Repository, pullRequest: PullRequest, page?: number) => {
  let url = createChangesetUrl(
    repository,
    pullRequest.sourceRevision || pullRequest.source,
    pullRequest.targetRevision || pullRequest.target
  );
  if (url && page) {
    url += `?page=${page - 1}`;
  }

  const { error, isLoading, data } = useQuery<ChangesetCollection, Error>(
    [...prQueryKey(repository, pullRequest?.id || pullRequest.source + pullRequest.target), "changesets", page || ""],
    () => {
      if (!url) {
        throw new Error("Could not fetch pull request changesets because link is missing");
      }

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
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not fetch conflicts for pull request without id");
  }

  const { error, isLoading, data } = useQuery<Conflicts, Error>([...prQueryKey(repository, id), "conflicts"], () => {
    return apiClient
      .post(
        requiredLink(pullRequest, "mergeConflicts"),
        { sourceRevision: pullRequest.source, targetRevision: pullRequest.target },
        "application/vnd.scmm-mergeCommand+json"
      )
      .then(r => r.json());
  });

  return {
    error,
    isLoading,
    data
  };
};

export const useCheckPullRequest = (
  repository: Repository,
  pullRequest: PullRequest,
  callback?: (checkResult: CheckResult) => void
) => {
  const id = pullRequest.id ? `${pullRequest.id}:${pullRequest.target}` : `${pullRequest.source}:${pullRequest.target}`;

  const { error, data } = useQuery<CheckResult, Error>(
    [...prQueryKey(repository, id), "check"],
    () => {
      const link = (pullRequest?._links?.check as Link)?.href ?? requiredLink(repository, "pullRequestCheck");
      return apiClient
        .get(
          link + `?source=${encodeURIComponent(pullRequest.source)}&target=${encodeURIComponent(pullRequest.target)}`
        )
        .then(r => r.json());
    },
    {
      onSuccess: result => {
        if (callback) {
          callback(result);
        }
      }
    }
  );

  return {
    error,
    data
  };
};

export const useMergeDryRun = (
  repository: Repository,
  pullRequest: PullRequest,
  callback?: (targetOrSourceBranchDeleted: boolean) => void
) => {
  const id = pullRequest.id || pullRequest.source + pullRequest.target;

  const { error, data, isLoading } = useQuery<MergeCheck, Error>(
    prMergeCheckQueryKey(repository, id),
    () => {
      return apiClient.post((pullRequest._links.mergeCheck as Link).href).then(r => r.json());
    },
    {
      onSuccess: () => {
        if (callback) {
          callback(false);
        }
      },
      onError: err => {
        if (err instanceof NotFoundError && callback) {
          callback(true);
        }
        if (err instanceof ConflictError) {
          return {
            mergeObstacles: data ? data.mergeObstacles : [],
            hasConflicts: true,
            mergePreventReason: data?.mergePreventReasons
          };
        }
      },
      enabled: !!pullRequest?._links?.mergeCheck
    }
  );

  return {
    isLoading,
    error,
    data
  };
};

export function getMergeStrategyInfo(url: string) {
  return apiClient.get(url).then(response => response.json());
}

export const useSourceBranch = (repository: Repository, pullRequest: PullRequest) => {
  const { data, isLoading } = useBranches(repository);
  const branch = !isLoading && data?._embedded?.branches.find(b => b.name === pullRequest.source);
  return { sourceBranch: branch, isLoading };
};

function createIncomingUrl(repository: Repository, linkName: string, source: string, target: string) {
  const link = repository._links[linkName];
  if ((link as Link)?.templated) {
    return (link as Link).href
      .replace("{source}", encodeURIComponent(source))
      .replace("{target}", encodeURIComponent(target));
  } else {
    return (link as Link).href;
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

export function evaluateTagColor(status?: string) {
  if (status === "MERGED") {
    return "success";
  } else if (status === "REJECTED") {
    return "danger";
  }
  return "light";
}

const requiredLink = (halObject: HalRepresentation, linkName: string): string => {
  if (!halObject._links[linkName]) {
    throw new Error("Could not find link: " + linkName);
  }
  return (halObject._links[linkName] as Link).href;
};
