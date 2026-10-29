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

import React, { FC, PropsWithChildren } from "react";
import { Branch, Repository } from "@scm-manager/ui-types";
import { urls } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Navigate, Route, Routes, useMatch } from "react-router";
import RootComments from "./comment/RootCommentContainer";
import { MergePreventReason, PullRequest } from "./types/PullRequest";
import DiffRoute from "./diff/DiffRoute";
import MergeConflicts from "./MergeConflicts";
import RootChangesContainer from "./change/RootChangesContainer";
import { SINGLE_REPOSITORY_BASE_ROUTE, StatusIcon } from "@scm-manager/ui-core";
import {
  PAGE_BASE,
  PULL_REQUEST_CHANGE_BASE,
  PULL_REQUEST_CHANGESET_BASE,
  PULL_REQUEST_COMMENT_BASE,
  PULL_REQUEST_CONFLICT_BASE,
  PULL_REQUEST_DIFF_BASE,
  SINGLE_PULL_REQUEST_BASE,
  usePullRequestMatchRoute,
} from "./routes";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
  status?: string;
  mergeHasNoConflict: boolean;
  mergePreventReasons: MergePreventReason[];
  targetBranchDeleted: boolean;
  shouldFetchChangesets?: boolean;
  sourceBranch?: Branch;
  stickyHeaderHeight: number;
};

type SingleTabProps = PropsWithChildren<{
  name: string;
  activeTab: (type: string) => boolean;
}>;

const SingleTab: FC<SingleTabProps> = ({ name, activeTab, children }) => {
  return (
    <li
      className={activeTab(name) ? "is-active" : ""}
      id={`tab-${name}`}
      role="tab"
      aria-selected={activeTab(name)}
      aria-controls={`tabpanel-${name}`}
    >
      {children}
    </li>
  );
};

type TabProps = {
  isClosed: boolean;
  baseURL: string;
  activeTab: (type: string) => boolean;
  pullRequest: PullRequest;
  targetBranchDeleted: boolean;
  mergeHasNoConflict: boolean;
};

const Tabs: FC<TabProps> = ({ isClosed, pullRequest, activeTab, baseURL, targetBranchDeleted, mergeHasNoConflict }) => {
  const [t] = useTranslation("plugins");

  let changesetTab = null;
  let diffTab = null;
  let conflictsTab = null;
  if (!isClosed || (pullRequest.sourceRevision && pullRequest.targetRevision)) {
    changesetTab = (
      <SingleTab name="changesets" activeTab={activeTab}>
        <Link to={`${baseURL}/changesets/`}>{t("scm-review-plugin.pullRequest.tabs.commits")}</Link>
      </SingleTab>
    );

    diffTab = (
      <SingleTab name="diff" activeTab={activeTab}>
        <Link to={`${baseURL}/diff/`}>{t("scm-review-plugin.pullRequest.tabs.diff")}</Link>
      </SingleTab>
    );

    conflictsTab = !mergeHasNoConflict && (
      <SingleTab name="conflicts" activeTab={activeTab}>
        <Link to={`${baseURL}/conflicts/`}>
          {t("scm-review-plugin.pullRequest.tabs.conflicts")}
          <StatusIcon className="ml-2" variant="warning" iconSize="xs" />
        </Link>
      </SingleTab>
    );
  }

  const commentTab = pullRequest._links?.comments ? (
    <SingleTab name="comments" activeTab={activeTab}>
      <Link to={`${baseURL}/comments/`}>{t("scm-review-plugin.pullRequest.tabs.comments")}</Link>
    </SingleTab>
  ) : null;

  const prChangesTab = pullRequest._links?.changes ? (
    <SingleTab name="changes" activeTab={activeTab}>
      <Link to={`${baseURL}/changes/`}>{t("scm-review-plugin.pullRequest.tabs.changes")}</Link>
    </SingleTab>
  ) : null;

  return (
    <div className="tabs">
      <ul role="tablist">
        {commentTab}
        {changesetTab}
        {prChangesTab}
        {!targetBranchDeleted && diffTab}
        {conflictsTab}
      </ul>
    </div>
  );
};

type RouteProps = {
  repository: Repository;
  pullRequest: PullRequest;
  isClosed: boolean;
  baseURL: string;
  mergeHasNoConflict: boolean;
  mergePreventReasons: MergePreventReason[];
  source: string;
  target: string;
  shouldFetchChangesets?: boolean;
  sourceBranch?: Branch;
  stickyHeaderHeight: number;
};

const PullRequestRoutes: FC<RouteProps> = ({
  repository,
  source,
  target,
  isClosed,
  pullRequest,
  baseURL,
  mergeHasNoConflict,
  mergePreventReasons,
  shouldFetchChangesets,
  sourceBranch,
  stickyHeaderHeight,
}) => {
  const match = useMatch(
    `/${SINGLE_REPOSITORY_BASE_ROUTE}/${repository.namespace}/${repository.name}/${SINGLE_PULL_REQUEST_BASE}/:id`,
  );
  if (match) {
    return (
      <Navigate replace to={urls.concat(baseURL + "/" + pullRequest.id, pullRequest ? "comments" : "changesets")} />
    );
  }

  let routeComments = null;
  let routeChangeset = null;
  let routeDiff = null;
  let routeConflicts = null;
  let routePrChanges = null;

  if (!isClosed || (pullRequest.sourceRevision && pullRequest.targetRevision)) {
    const sourceRevision = isClosed && pullRequest.sourceRevision ? pullRequest.sourceRevision : source;
    const targetRevision = isClosed && pullRequest.targetRevision ? pullRequest.targetRevision : target;

    routeChangeset = (
      <Route path={PULL_REQUEST_CHANGESET_BASE}>
        <Route
          index
          element={
            <div id="tabpanel-changesets" role="tabpanel" aria-labelledby="tab-changesets">
              <Changesets
                repository={repository}
                pullRequest={pullRequest}
                shouldFetchChangesets={shouldFetchChangesets}
              />
            </div>
          }
        />
        <Route
          path={PAGE_BASE}
          element={
            <div id="tabpanel-changesets" role="tabpanel" aria-labelledby="tab-changesets">
              <Changesets
                repository={repository}
                pullRequest={pullRequest}
                shouldFetchChangesets={shouldFetchChangesets}
              />
            </div>
          }
        />
      </Route>
    );

    routeDiff = (
      <Route
        path={PULL_REQUEST_DIFF_BASE}
        element={
          <div id="tabpanel-diff" role="tabpanel" aria-labelledby="tab-diff">
            <DiffRoute
              repository={repository}
              pullRequest={pullRequest}
              source={sourceRevision}
              target={targetRevision}
              sourceBranch={sourceBranch}
              stickyHeaderHeight={stickyHeaderHeight}
              mergePreventReasons={mergePreventReasons}
            />
          </div>
        }
      />
    );

    routeComments = (
      <Route
        path={PULL_REQUEST_COMMENT_BASE}
        element={
          <div id="tabpanel-comments" role="tabpanel" aria-labelledby="tab-comments">
            {pullRequest ? <RootComments repository={repository} pullRequest={pullRequest} /> : null}
          </div>
        }
      />
    );

    routePrChanges = (
      <Route
        path={PULL_REQUEST_CHANGE_BASE}
        element={
          <div id="tabpanel-conflicts" role="tabpanel" aria-labelledby="tab-conflicts">
            <RootChangesContainer pullRequest={pullRequest} repository={repository} />
          </div>
        }
      />
    );

    routeConflicts = !mergeHasNoConflict && (
      <Route
        path={PULL_REQUEST_CONFLICT_BASE}
        element={
          <div id="tabpanel-conflicts" role="tabpanel" aria-labelledby="tab-conflicts">
            <MergeConflicts
              repository={repository}
              pullRequest={pullRequest}
              mergePreventReasons={mergePreventReasons}
            />
          </div>
        }
      />
    );
  }

  return (
    <Routes>
      {routeComments}
      {routeChangeset}
      {routeDiff}
      {routeConflicts}
      {routePrChanges}
    </Routes>
  );
};

const PullRequestInformation: FC<Props> = ({
  pullRequest,
  mergeHasNoConflict,
  mergePreventReasons,
  status,
  targetBranchDeleted,
  sourceBranch,
  ...restProps
}) => {
  const match = usePullRequestMatchRoute();
  const baseURL = match.pathnameBase.substring(0, match.pathnameBase.lastIndexOf("/"));

  const activeTab = (suffix: string) => {
    // @ts-ignore
    return match.params.tab === suffix;
  };
  const isClosedPullRequest = status === "MERGED" || status === "REJECTED";

  return (
    <>
      <Tabs
        isClosed={isClosedPullRequest}
        baseURL={baseURL}
        activeTab={activeTab}
        pullRequest={pullRequest}
        targetBranchDeleted={targetBranchDeleted}
        mergeHasNoConflict={mergeHasNoConflict}
      />
      <PullRequestRoutes
        {...restProps}
        pullRequest={pullRequest}
        isClosed={isClosedPullRequest}
        mergeHasNoConflict={mergeHasNoConflict}
        mergePreventReasons={mergePreventReasons}
        baseURL={baseURL}
        sourceBranch={sourceBranch}
      />
    </>
  );
};

export default PullRequestInformation;
