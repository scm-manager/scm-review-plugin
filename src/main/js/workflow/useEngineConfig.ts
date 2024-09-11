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

import { apiClient, ApiResult, objectLink } from "@scm-manager/ui-api";
import { EngineConfiguration } from "../types/EngineConfig";
import { Repository } from "@scm-manager/ui-types";
import { useQuery } from "react-query";

export default (repository: Repository): ApiResult<EngineConfiguration> => {
  const configLink = objectLink(repository, "effectiveWorkflowConfig") || "";
  return useQuery<EngineConfiguration, Error>(
    ["repository", repository.namespace, repository.name, "effectiveWorkflowConfig"],
    () => apiClient.get(configLink).then(response => response.json()),
    { enabled: !!configLink }
  );
};
