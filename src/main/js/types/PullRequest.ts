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

import {
  DisplayedUser,
  HalRepresentation,
  HalRepresentationWithEmbedded,
  Links,
  PagedCollection
} from "@scm-manager/ui-types";
import { ReactText } from "react";

export type PullRequestStatus = "OPEN" | "MERGED" | "REJECTED" | "DRAFT";

export type Reviewer = DisplayedUser & {
  approved: boolean;
};

export type BasicPullRequest = {
  source: string;
  target: string;
  title: string;
};

export type PullRequest = BasicPullRequest &
  HalRepresentationWithEmbedded<{ availableLabels: { availableLabels: string[] } }> & {
    description?: string;
    author?: DisplayedUser;
    reviser?: DisplayedUser;
    closeDate?: string;
    id?: string;
    creationDate?: string;
    reviewer?: Reviewer[];
    labels: string[];
    status: PullRequestStatus;
    tasks?: Tasks;
    sourceRevision?: string;
    targetRevision?: string;
    markedAsReviewed?: string[];
    emergencyMerged?: boolean;
    ignoredMergeObstacles?: string[];
    shouldDeleteSourceBranch: boolean;
    initialTasks: string[];
  };

export type Location = {
  file: string;
  hunk?: string;
  oldLineNumber?: number;
  newLineNumber?: number;
};

export type CommentType = "COMMENT" | "TASK_TODO" | "TASK_DONE";

export type BasicComment = {
  comment?: string;
  id?: string;
  type: CommentType;
  mentions: Mention[];
  location?: Location;
};

export type Comment = BasicComment & {
  author: DisplayedUser;
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
  id: ReactText;
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

export type PagedPullRequestCollection = PagedCollection<PullRequestCollection>;

export type PullRequestCollection = HalRepresentation & {
  _embedded: {
    pullRequests: PullRequest[];
  };
};

export type Comments = HalRepresentation & {
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
  status: "PR_VALID" | "BRANCHES_NOT_DIFFER" | "PR_ALREADY_EXISTS";
};
