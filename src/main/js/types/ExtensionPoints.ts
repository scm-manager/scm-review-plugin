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

import { ExtensionPointDefinition, RenderableExtensionPointDefinition } from "@scm-manager/ui-extensions";
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
 * @deprecated Replaced by {@link PullRequestListDetailExtension} since 2.29.0
 */
export type PullRequestTableColumn = ExtensionPointDefinition<
  "pull-requests.table.column",
  (props: FactoryProps) => ReactElement<PullRequestTableExtension>,
  FactoryProps
>;

/**
 * @since 2.29.0
 */
export type PullRequestListDetailExtension = RenderableExtensionPointDefinition<
  "pull-requests.list.detail",
  {
    repository: Repository;
    pullRequest: PullRequest;
  }
>;
