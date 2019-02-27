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

export type BasicComment = {
  comment: string,
};

export type Comment = BasicComment & {
  author: string,
  date: string,
  _links: Links
};

export type LineComment = Comment & {
  line: number
}

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
