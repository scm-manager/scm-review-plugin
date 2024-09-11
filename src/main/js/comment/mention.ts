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

import { SuggestionDataItem } from "react-mentions";
import { SelectValue } from "@scm-manager/ui-types";

export const getUserSuggestions = (
  userSuggestions: (p: string) => Promise<SelectValue[]>,
  query: string,
  callback: (data: SuggestionDataItem[]) => void
) => {
  if (query && query.length > 1) {
    return userSuggestions(query)
      .then(suggestions => {
        return suggestions.map((s: SelectValue) => {
          return {
            id: s.value.id,
            display: s.value.displayName
          };
        });
      })
      .then(callback);
  }
};
