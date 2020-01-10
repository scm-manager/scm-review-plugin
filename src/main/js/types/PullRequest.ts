import { Collection, DisplayedUser, Links } from "@scm-manager/ui-types";

export type Reviewer = DisplayedUser & {
  approved: boolean;
};

export type BasicPullRequest = {
  source: string;
  target: string;
  title: string;
};

export type PullRequest = BasicPullRequest & {
  description?: string;
  author: DisplayedUser;
  id: string;
  creationDate: string;
  reviewer: Reviewer[];
  status: string;
  _links: Links;
  tasks: Tasks;
  sourceRevision: string;
  targetRevision: string;
};

export type Location = {
  file: string;
  hunk?: string;
  oldLineNumber?: number;
  newLineNumber?: number;
};

export type BasicComment = {
  comment: string;
  id: string;
  type: string;
};

export type Comment = BasicComment & {
  author: DisplayedUser;
  location?: Location;
  date: string;
  outdated: boolean;
  systemComment: boolean;
  file?: string;
  lineId?: string;
  replies: Comment[];
  context?: Context;
  _links: Links;
  _embedded?: { [key: string]: any };
};

export type Context = {
  lines: ContextLine[];
};

export type ContextLine = {
  content: string;
  newLineNumber?: number;
  oldLineNumber?: number;
};

export type Reply = BasicComment & {
  author: DisplayedUser;
  date: string;
  _links: Links;
};

export type PullRequestCollection = Collection & {
  _embedded: {
    pullRequests: PullRequest[];
  };
};

export type Comments = Collection & {
  _embedded: {
    pullRequestComments: Comment[];
  };
};

export type Transition = {
  id: string;
  transition: string;
  date: string;
  user: DisplayedUser;
};

export type PossibleTransition = {
  name: string;
  _links: Links;
};

export type MergeCommit = {
  commitMessage: string;
  author: DisplayedUser;
  shouldDeleteSourceBranch: boolean;
};

export type MergeCheck = {
  hasConflicts: boolean;
  mergeObstacles: MergeObstacle[];
};

export type MergeObstacle = {
  message: string;
  key: string;
};

export type Conflict = {
  type: string;
  path: string;
  diff: string;
};

export type Conflicts = {
  conflicts: Conflict[];
};

export type Tasks = {
  todo: number;
  done: number;
};
