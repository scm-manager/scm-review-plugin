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
