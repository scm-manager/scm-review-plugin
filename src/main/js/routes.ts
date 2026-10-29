/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import { SINGLE_REPOSITORY_BASE_ROUTE } from "@scm-manager/ui-core";
import { Repository } from "@scm-manager/ui-types";
import { useMatch } from "react-router";
import { PullRequest } from "./types/PullRequest";

const NAMESPACE_PATTERN = ":namespace";
const NAME_PATTERN = ":name";

export const PAGE_BASE = ":page";
export const PULL_REQUEST_COLLECTION_BASE = "pull-requests";
export const SINGLE_PULL_REQUEST_BASE = "pull-request";
export const PULL_REQUEST_ID = ":pullRequestId";
export const CREATE_PULL_REQUEST_BASE = "add";
export const PULL_REQUEST_CHANGESET_BASE = "changesets";
export const PULL_REQUEST_DIFF_BASE = "diff";
export const PULL_REQUEST_COMMENT_BASE = "comments";
export const PULL_REQUEST_CHANGE_BASE = "changes";
export const PULL_REQUEST_CONFLICT_BASE = "conflicts";
export const PULL_REQUEST_EDIT_BASE = "edit";

export const resolvePullRequestOverview = (repository: Repository) => {
  return `/${SINGLE_REPOSITORY_BASE_ROUTE}/${repository.namespace}/${repository.name}/${PULL_REQUEST_COLLECTION_BASE}`;
};

export const resolvePagedPullRequestOverview = (repository: Repository, page: number) => {
  return `${resolvePullRequestOverview(repository)}/${page}`;
};

export const resolveCreatePullRequest = (repository: Repository, tab: string) => {
  return `${resolvePullRequestOverview(repository)}/${CREATE_PULL_REQUEST_BASE}/${tab}`;
};

export const resolveSinglePullRequest = (repository: Repository, pullRequest: PullRequest, tab: string) => {
  return `/${SINGLE_REPOSITORY_BASE_ROUTE}/${repository.namespace}/${repository.name}/${SINGLE_PULL_REQUEST_BASE}/${pullRequest.id}/${tab}`;
};

const CREATE_PULL_REQUEST_PATTERN = `${SINGLE_REPOSITORY_BASE_ROUTE}/${NAMESPACE_PATTERN}/${NAME_PATTERN}/${PULL_REQUEST_COLLECTION_BASE}/${CREATE_PULL_REQUEST_BASE}/:tab/*`;
const SINGLE_PULL_REQUEST_PATTERN = `${SINGLE_REPOSITORY_BASE_ROUTE}/${NAMESPACE_PATTERN}/${NAME_PATTERN}/${SINGLE_PULL_REQUEST_BASE}/${PULL_REQUEST_ID}/:tab/*`;

export const usePullRequestMatchRoute = () => {
  const createPullRequestMatch = useMatch(CREATE_PULL_REQUEST_PATTERN);
  const singlePullRequestMatch = useMatch(SINGLE_PULL_REQUEST_PATTERN);
  const redirectPullRequestMatch = useMatch(
    `${SINGLE_REPOSITORY_BASE_ROUTE}/${NAMESPACE_PATTERN}/${NAME_PATTERN}/${SINGLE_PULL_REQUEST_BASE}/${PULL_REQUEST_ID}`,
  );

  if (createPullRequestMatch) {
    return createPullRequestMatch;
  }

  if (singlePullRequestMatch) {
    return singlePullRequestMatch;
  }

  if (redirectPullRequestMatch) {
    return redirectPullRequestMatch;
  }

  throw new Error("URL does not match expected pull request pattern");
};
