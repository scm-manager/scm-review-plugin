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

export type ProtectionBypass = {
  name: string;
  group: boolean;
};

export const MERGE_STRATEGIES = ["MERGE_COMMIT", "FAST_FORWARD_IF_POSSIBLE", "SQUASH", "REBASE"] as const;
export type MergeStrategy = typeof MERGE_STRATEGIES[number];

export type BranchProtection = {
  branch: string;
  path: string;
};

export type Config = {
  defaultMergeStrategy: MergeStrategy;
  deleteBranchOnMerge: boolean;
  disableRepositoryConfiguration?: boolean;
  overwriteParentConfig?: boolean;
  restrictBranchWriteAccess: boolean;
  protectedBranchPatterns: BranchProtection[];
  branchProtectionBypasses: ProtectionBypass[];
  preventMergeFromAuthor: boolean;
  defaultReviewers: string[];
  labels: string[];
  defaultTasks: string[];
  overwriteDefaultCommitMessage: boolean;
  commitMessageTemplate: string;
};
