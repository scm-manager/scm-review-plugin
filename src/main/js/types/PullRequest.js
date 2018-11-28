// @flow
import type { Links } from "@scm-manager/ui-types";

export type BasicPullRequest = {
  source: string,
  target: string,
  title: string
};

export type PullRequest = BasicPullRequest & {
  description?: string,
  author: string,
  id: string,
  creationDate: string,
  _links: Links
}
