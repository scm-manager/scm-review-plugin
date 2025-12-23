/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import {
  DisplayedUser,
  HalRepresentation,
  HalRepresentationWithEmbedded,
  Links,
  PagedCollection,
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
  systemCommentParameters?: { [key: string]: string };
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

export type CommentImage = {
  fileHash: string;
  file: File;
  filetype: string;
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

export type MergePreventReason = {
  type: "FILE_CONFLICTS" | "EXTERNAL_MERGE_TOOL";
  affectedPaths?: string[];
};

export type MergeCheck = {
  hasConflicts: boolean;
  mergeObstacles: MergeObstacle[];
  mergePreventReasons?: MergePreventReason[];
  sourceBranchMissing: boolean;
  targetBranchMissing: boolean;
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

export type Banner = HalRepresentation & {
  branch: string;
  pushedAt: Date;
};
