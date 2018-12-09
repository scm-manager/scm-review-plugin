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
  state: string,
  _links: Links
}

export type Comment = {
  comment: string,
  author: string,
  date: string,
  _links: Links
}

export type PullRequestCollection = Collection & {
  _embedded: {
    pullRequests: PullRequest[]
  }
}

export type Comments = Collection & {
  _embedded: {
    pullRequestComments: Comment[]
  }
}
