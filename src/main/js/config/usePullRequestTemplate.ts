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

import { DisplayedUser, HalRepresentation, Repository } from "@scm-manager/ui-types";
import { apiClient, ApiResult, getResponseJson, requiredLink } from "@scm-manager/ui-api";
import { useQuery } from "react-query";

export type PullRequestTemplate = HalRepresentation & {
  defaultReviewers: DisplayedUser[];
  availableLabels: string[];
  title?: string;
  description?: string;
  defaultTasks: string[];
  shouldDeleteSourceBranch: boolean;
};

export default function usePullRequestTemplate(
  repository: Repository,
  source: string,
  target: string
): ApiResult<PullRequestTemplate> {
  const link = requiredLink(repository, "pullRequestTemplate");
  return useQuery<PullRequestTemplate, Error>(
    ["repository", repository.namespace, repository.name, "pullRequestTemplate", source, target],
    () => apiClient.get(`${link}?source=${source}&target=${target}`).then(getResponseJson)
  );
}
