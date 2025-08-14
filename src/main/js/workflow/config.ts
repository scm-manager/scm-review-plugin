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

import { ApiResult } from "@scm-manager/ui-api";
import { useQuery } from "react-query";
import { apiClient } from "@scm-manager/ui-components";
import { AvailableRules, EngineConfiguration } from "../types/EngineConfig";
import { Link } from "@scm-manager/ui-types";

export const useEngineConfigRules = (config: EngineConfiguration): ApiResult<AvailableRules> => {
  return useQuery<AvailableRules, Error>(["workflow", "available-rules"], () => {
    if (config._links.availableRules) {
      return apiClient.get((config._links.availableRules as Link).href).then(response => response.json());
    }
    return { rules: [] };
  });
};
