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

import { ExtensionPointDefinition } from "@scm-manager/ui-extensions";
import { ReactElement, ReactNode } from "react";
import { PullRequest } from "./PullRequest";
import { Repository } from "@scm-manager/ui-types";
import i18next from "i18next";

type PullRequestTableExtension = {
  header: ReactNode;
  createComparator?: (columnIndex: number) => (a: PullRequest, b: PullRequest) => number;
  ascendingIcon?: string;
  descendingIcon?: string;
  className?: string;
  children: (row: PullRequest, columnIndex: number) => ReactNode;
};

type FactoryProps = {
  repository: Repository;
  pullRequests: PullRequest[];
  t: typeof i18next.t;
};

/**
 * @since 2.19.0
 */
export type PullRequestTableColumn = ExtensionPointDefinition<
  "pull-requests.table.column",
  (props: FactoryProps) => ReactElement<PullRequestTableExtension>
>;
