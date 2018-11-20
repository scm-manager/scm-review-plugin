import type { PullRequest } from "./PullRequest";
import { apiClient } from "@scm-manager/ui-components";

export function createPullRequest(url: string, pullRequest: PullRequest) {
  return apiClient
    .post(url, pullRequest)
    .then(response => {
      return response;
    })
    .catch(cause => {
      const error = new Error(
        `could not create pull request: ${cause.message}`
      );
      return { error: error };
    });
}

export function getBranches(url: string) {
  return apiClient
    .get(url)
    .then(response => response.json())
    .then(collection => collection._embedded.branches)
    .then(branches => {
      return branches.map(b => b.name);
    })
    .catch(cause => {
      const error = new Error(`could not fetch branches: ${cause.message}`);
      return { error: error };
    });
}
