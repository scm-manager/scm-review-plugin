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

import { useQuery } from "react-query";
import { apiClient } from "@scm-manager/ui-components";
import { Result } from "../types/EngineConfig";
import { HalRepresentation, Link, Repository } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { prQueryKey } from "../pullRequest";

type StatusbarResult = HalRepresentation & {
  results: Result[];
};

export const useStatusbar = (repository: Repository, pullRequest: PullRequest) => {
  const id = pullRequest.id;

  if (!id) {
    throw new Error("Could not fetch statusbar for pull request without id");
  }

  const { error, isLoading, data } = useQuery<StatusbarResult | undefined, Error>(
    [...prQueryKey(repository, id), "statusbar"],
    () => {
      return apiClient.get((pullRequest._links.workflowResult as Link).href).then(response => response.json());
    },
    {
      enabled: !!pullRequest._links.workflowResult
    }
  );

  return {
    error,
    isLoading,
    data
  };
};
