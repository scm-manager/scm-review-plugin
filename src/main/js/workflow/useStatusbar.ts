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
