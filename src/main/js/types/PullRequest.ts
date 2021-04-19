/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
  reviser?: DisplayedUser;
  id: string;
  creationDate: string;
  reviewer: Reviewer[];
  status: string;
  _links: Links;
  tasks: Tasks;
  sourceRevision: string;
  targetRevision: string;
  markedAsReviewed: string[];
  emergencyMerged: boolean;
  ignoredMergeObstacles: string[];
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
  mentions: Mention[];
};

export type Comment = BasicComment & {
  author: DisplayedUser;
  location?: Location;
  date: string;
  outdated: boolean;
  systemComment: boolean;
  emergencyMerged: boolean;
  file?: string;
  lineId?: string;
  replies: Comment[];
  context?: Context;
  _links: Links;
  _embedded?: { [key: string]: any };
};

export type Mention = {
  id: string;
  displayName: string;
  mail: string;
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
  shouldDeleteSourceBranch: boolean;
  overrideMessage?: string;
};

export type MergeCheck = {
  hasConflicts: boolean;
  mergeObstacles: MergeObstacle[];
};

export type MergeObstacle = {
  message: string;
  key: string;
  overrideable: boolean;
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

export type CheckResult = {
  status: "PR_VALID" | "BRANCHES_NOT_DIFFER" | "PR_ALREADY_EXISTS"
};
