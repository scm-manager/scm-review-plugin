import {SuggestionDataItem} from "react-mentions";
import { apiClient } from "@scm-manager/ui-components";
import {Mention} from "../types/PullRequest";

export function mapAutocompleteToSuggestions(link: string, query: string, callback: (data: SuggestionDataItem[]) => void) {
  if (query && query.length > 1) {
    const url = link + "?q=";
    return apiClient
      .get(url + query)
      .then(response => response.json())
      .then(suggestions => {
        return suggestions.map((s: Mention) => {
          return { id: s.id, display: s.displayName, mail: s.mail };
        });
      })
      .then(callback);
  }
};
