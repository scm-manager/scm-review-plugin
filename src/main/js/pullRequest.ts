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
  Comments,
  Conflicts,
  MergeCheck,
  MergeCommit,
  PossibleTransition,
  PullRequest,
  PullRequestCollection
} from "./types/PullRequest";
import { apiClient, ConflictError, NotFoundError } from "@scm-manager/ui-components";
import { Changeset, HalRepresentation, Link, PagedCollection, Repository } from "@scm-manager/ui-types";
import { useMutation, useQuery, useQueryClient } from "react-query";
import { MergeStrategyInfo } from "./types/MergeStrategyInfo";

const CONTENT_TYPE_PULLREQUEST = "application/vnd.scmm-pullRequest+json;v=2";

// React-Query Hooks

export const invalidatePullRequest = (repository: Repository, pullRequestId?: string) => {
  if (!pullRequestId) {
    throw new Error("could not invalidate pull request without id");
  }

  const queryClient = useQueryClient();
  queryClient.invalidateQueries(prQueryKey(repository, pullRequestId));
};

export const prQueryKey = (repository: Repository, pullRequestId: string) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequestId];
};

const prsQueryKey = (repository: Repository, status?: string) => {
  return ["repository", repository.namespace, repository.name, "pull-requests", status || ""];
};

const prCommentsQueryKey = (repository: Repository, pullRequest: PullRequest) => {
  return ["repository", repository.namespace, repository.name, "pull-request", pullRequest.id, "comments"];
};

export const usePullRequest = (repository: Repository, pullRequestId?: string) => {
  if (!pullRequestId) {
    throw new Error("Could not fetch pull request without id");
  }

  if (!repository._links.pullRequest) {
    throw new Error("Could not fetch pull request because link is missing");
  }

  const { error, isLoading, data } = useQuery<PullRequest, Error>(prQueryKey(repository, pullRequestId), () =>
    apiClient.get((repository._links.pullRequest as Link).href + "/" + pullRequestId).then(response => response.json())
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
      if (!pr._links.update) {
        throw new Error("Could not update pull request because update link is missing");
      }

      return apiClient.put((pr._links.update as Link).href, pr, CONTENT_TYPE_PULLREQUEST);
    },
    {
      onSuccess: async () => {
        if (callback) {
          callback();
        }
        await queryClient.invalidateQueries(prQueryKey(repository, id));
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
  const { isLoading: approveLoading, error: approveError, mutate: approve } = useMutation<unknown, Error, void>(
    () => apiClient.post((pullRequest._links.approve as Link).href, {}),
    {
      onSuccess: async () => {
        await queryClient.invalidateQueries(prQueryKey(repository, id));
      }
    }
  );

  const { isLoading: disapproveLoading, error: disapproveError, mutate: disapprove } = useMutation<
    unknown,
    Error,
    void
  >(() => apiClient.post((pullRequest._links.disapprove as Link).href, {}), {
    onSuccess: async () => {
      await queryClient.invalidateQueries(prQueryKey(repository, id));
    }
  });

  return {
    isLoading: approveLoading || disapproveLoading,
    error: approveError || disapproveError,
    approve: pullRequest._links.approve ? () => approve() : undefined,
    disapprove: pullRequest._links.disapprove ? () => disapprove() : undefined
  };
};

export const useCreatePullRequest = (repository: Repository, callback?: (id: string) => void) => {
  const queryClient = useQueryClient();
  const { mutate, data, isLoading, error } = useMutation<PullRequest, Error, BasicPullRequest>(
    pr => {
      if (!repository._links.pullRequest) {
        throw new Error("Could not create pull request because create link is missing");
      }
      return createPullRequest((repository._links.pullRequest as Link).href, pr);
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
        queryClient.invalidateQueries(prsQueryKey(repository));
        return queryClient.invalidateQueries(prQueryKey(repository, id));
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
  const { mutate, isLoading, error } = useMutation<unknown, Error, PullRequest>(
    pr => {
      if (!pr._links.reject) {
        throw new Error("Could not reject pull request because reject link is missing");
      }
      return apiClient.post((pr._links.reject as Link).href, {});
    },
    {
      onSuccess: () => {
        queryClient.invalidateQueries(prsQueryKey(repository));
        return queryClient.invalidateQueries(prQueryKey(repository, id));
      }
    }
  );
  return {
    reject: (pr: PullRequest) => mutate(pr),
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
        queryClient.invalidateQueries(prsQueryKey(repository));
        return queryClient.invalidateQueries(prQueryKey(repository, id));
      }
    }
  );
  return {
    merge: (request: MergeRequest) => mutate(request),
    isLoading,
    error
  };
};

export const usePullRequests = (repository: Repository, status?: string) => {
  let url = (repository._links.pullRequest as Link).href;
  if (url && status) {
    url += "?status=" + status;
  }

  const { error, isLoading, data } = useQuery<PullRequestCollection, Error>(
    ["repository", repository.namespace, repository.name, "pull-requests", status || ""],
    () => {
      if (!url) {
        throw new Error("Could not fetch pull requests because url is not defined");
      }
      return apiClient.get(url).then(response => response.json());
    }
  );

  return {
    error,
    isLoading,
    data
  };
};

export const useDeleteComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, Comment>(
    comment => {
      if (!comment._links.delete) {
        throw new Error("Could not delete comment because delete link is missing");
      }
      return apiClient.delete((comment._links.delete as Link).href);
    },
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
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
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<{}, Error, Comment>(
    comment => {
      if (!comment._links.update) {
        throw new Error("Could not update comment because update link is missing");
      }
      return apiClient.put((comment._links.update as Link).href, comment);
    },
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    update: (comment: Comment) => mutate(comment),
    isLoading,
    error
  };
};

export const useTransformComment = (repository: Repository, pullRequest: PullRequest) => {
  const queryClient = useQueryClient();
  const { mutate, isLoading, error } = useMutation<unknown, Error, PossibleTransition>(
    transition => {
      if (!transition._links.transform) {
        throw new Error("Could not transform comment because transform link is missing");
      }
      return apiClient.post((transition._links.transform as Link).href, transition);
    },
    {
      onSuccess: () => {
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
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

export const useCreateComment = (repository: Repository, pullRequest: PullRequest) => {
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
        return queryClient.invalidateQueries(prCommentsQueryKey(repository, pullRequest));
      }
    }
  );
  return {
    create: (url: string, comment: BasicComment) => mutate({ url, comment }),
    isLoading,
    error
  };
};

export const useComments = (repository: Repository, pullRequest: PullRequest) => {
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
    if (!pullRequest._links.mergeConflicts) {
      throw new Error("Could not fetch pull request conflicts because link is missing");
    }

    return apiClient
      .post(
        (pullRequest._links.mergeConflicts as Link).href,
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
  const id = pullRequest.id || pullRequest.source + pullRequest.target;

  const { error, data } = useQuery<CheckResult, Error>(
    [...prQueryKey(repository, id), "check"],
    () => {
      if (!repository._links.pullRequestCheck) {
        throw new Error("Could not check pull request because check link is missing");
      }

      return apiClient
        .get(
          (repository._links.pullRequestCheck as Link)?.href +
            `?source=${pullRequest.source}&target=${pullRequest.target}`
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
  callback?: (targetBranchDeleted: boolean) => void
) => {
  const id = pullRequest.id || pullRequest.source + pullRequest.target;

  const { error, data, isLoading } = useQuery<MergeCheck, Error>(
    [...prQueryKey(repository, id), "merge-check"],
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
            hasConflicts: true
          };
        }
      },
      enabled: pullRequest.status === "OPEN" && !!pullRequest?._links?.mergeCheck
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

export function evaluateTagColor(pullRequest: PullRequest) {
  if (pullRequest.status === "MERGED") {
    return "success";
  } else if (pullRequest.status === "REJECTED") {
    return "danger";
  }
  return "light";
}
