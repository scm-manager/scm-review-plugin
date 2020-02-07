import {SuggestionDataItem} from "react-mentions";
import { apiClient } from "@scm-manager/ui-components";
import { AutocompleteObject } from "@scm-manager/ui-types";

export function mapAutocompleteToSuggestions(link: string, query: string, callback: (data: SuggestionDataItem[]) => void) {
  if (query && query.length > 1) {
    const url = link + "?q=";
    return apiClient
      .get(url + query)
      .then(response => response.json())
      .then(suggestions => {
        return suggestions.map((s: AutocompleteObject) => {
          return { id: s.id, display: s.displayName };
        });
      })
      .then(callback);
  }
};
