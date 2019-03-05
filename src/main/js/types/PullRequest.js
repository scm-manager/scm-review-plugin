// @flow
import type { Collection, Links } from "@scm-manager/ui-types";

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
  status: string,
  _links: Links
};

export type Location = {
  file: string,
  hunk: string,
  changeId: string
};

export type BasicComment = {
  comment: string,
  location?: Location
};

export type Comment = BasicComment & {
  author: string,
  date: string,
  file?: string,
  lineId?: string,
  _links: Links
};

export type PullRequestCollection = Collection & {
  _embedded: {
    pullRequests: PullRequest[]
  }
};

export type Comments = Collection & {
  _embedded: {
    pullRequestComments: Comment[]
  }
};
