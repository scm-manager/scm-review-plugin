// @flow
import type { DisplayedUser, Links, Collection } from "@scm-manager/ui-types";

export type BasicPullRequest = {
  source: string,
  target: string,
  title: string
};

export type PullRequest = BasicPullRequest & {
  description?: string,
  author: DisplayedUser,
  id: string,
  creationDate: string,
  reviewer: DisplayedUser[],
  status: string,
  _links: Links
};

export type Location = {
  file: string,
  hunk?: string,
  oldLineNumber?: number,
  newLineNumber?: number
};

export type BasicComment = {
  comment: string,
  id: string,
  type: string
};

export type Comment = BasicComment & {
  author: DisplayedUser,
  location?: Location,
  date: string,
  outdated: boolean,
  systemComment: boolean,
  file?: string,
  lineId?: string,
  replies: Comment[],
  context?: Context,
  _links: Links
};

export type Context = {
  lines: ContextLine[]
};

export type ContextLine = {
  content: string,
  newLineNumber?: number,
  oldLineNumber?: number
};

export type Reply = BasicComment & {
  author: DisplayedUser,
  date: string,
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

export type PossibleTransition = {
  name: string,
  _links: Links
};
