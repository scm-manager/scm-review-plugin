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

import { HalRepresentation } from "@scm-manager/ui-types";

export type PullRequestChange = {
  prId: string;
  username?: string;
  displayName?: string;
  mail?: string;
  changedAt: string;
  previousValue?: string;
  currentValue?: string;
  property: string;
  additionalInfo?: Record<string, string>;
} & HalRepresentation;

export const DEFAULT_PROPERTIES = {
  SOURCE_BRANCH: "SOURCE_BRANCH",
  TARGET_BRANCH: "TARGET_BRANCH",
  TITLE: "TITLE",
  DESCRIPTION: "DESCRIPTION",
  SUBSCRIBER: "SUBSCRIBER",
  LABELS: "LABELS",
  REVIEW_MARKS: "REVIEW_MARKS",
  DELETE_SOURCE_BRANCH_AFTER_MERGE: "DELETE_SOURCE_BRANCH_AFTER_MERGE",
  REVIEWER: "REVIEWER",
  COMMENT: "COMMENT",
  REPLY: "REPLY",
  TASK: "TASK",
  COMMENT_TYPE: "COMMENT_TYPE",
  PR_STATUS: "PR_STATUS",
  SOURCE_BRANCH_REVISION: "SOURCE_BRANCH_REVISION"
};

export const DEFAULT_ADDITIONAL_INFOS = {
  CURRENT_USERNAME: "currentUsername",
  CURRENT_APPROVED: "currentApproved",
  PREVIOUS_USERNAME: "previousUsername",
  PREVIOUS_APPROVED: "previousApproved",
  IS_SYSTEM_COMMENT: "isSystemComment",
  COMMENT_VALUE: "comment",
  FILE: "file",
  USER: "user",
  REJECTION_CAUSE: "rejectionCause"
};
