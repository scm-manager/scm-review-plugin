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

import React, { FC } from "react";
import { Branch, Repository } from "@scm-manager/ui-types";
import { Icon, urls } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, Switch, useLocation, useRouteMatch } from "react-router-dom";
import RootComments from "./comment/RootCommentContainer";
import { MergePreventReason, PullRequest } from "./types/PullRequest";
import DiffRoute from "./diff/DiffRoute";
import MergeConflicts from "./MergeConflicts";
import RootChangesContainer from "./change/RootChangesContainer";

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

export function isUrlSuffixMatching(baseURL: string, url: string, suffix: string) {
  let strippedUrl = url.substring(baseURL.length);
  if (strippedUrl.startsWith("/")) {
    strippedUrl = strippedUrl.substring(1);
  }
  const slash = strippedUrl.indexOf("/");
  if (slash >= 0) {
    strippedUrl = strippedUrl.substring(0, slash);
  }
  return strippedUrl === suffix;
}

type SingleTabProps = {
  name: string;
  activeTab: (type: string) => boolean;
};

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
          <Icon className="ml-2" color="warning" name="exclamation-triangle" />
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

const Routes: FC<RouteProps> = ({
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
  stickyHeaderHeight
}) => {
  let routeComments = null;
  let routeChangeset = null;
  let routeChangesetPagination = null;
  let routeDiff = null;
  let routeConflicts = null;
  let routePrChanges = null;
  if (!isClosed || (pullRequest.sourceRevision && pullRequest.targetRevision)) {
    const sourceRevision = isClosed && pullRequest.sourceRevision ? pullRequest.sourceRevision : source;
    const targetRevision = isClosed && pullRequest.targetRevision ? pullRequest.targetRevision : target;
    routeComments = (
      <Route path={`${baseURL}/comments`} exact>
        <div id="tabpanel-comments" role="tabpanel" aria-labelledby="tab-comments">
          {pullRequest ? <RootComments repository={repository} pullRequest={pullRequest} /> : null}
        </div>
      </Route>
    );
    routeChangeset = (
      <Route path={`${baseURL}/changesets`} exact>
        <div id="tabpanel-changesets" role="tabpanel" aria-labelledby="tab-changesets">
          <Changesets repository={repository} pullRequest={pullRequest} shouldFetchChangesets={shouldFetchChangesets} />
        </div>
      </Route>
    );
    routeChangesetPagination = (
      <Route path={`${baseURL}/changesets/:page`} exact>
        <div id="tabpanel-changesets" role="tabpanel" aria-labelledby="tab-changesets">
          <Changesets repository={repository} pullRequest={pullRequest} shouldFetchChangesets={shouldFetchChangesets} />
        </div>
      </Route>
    );
    routeDiff = (
      <Route path={`${baseURL}/diff`} exact>
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
      </Route>
    );
    routeConflicts = !mergeHasNoConflict && (
      <Route path={`${baseURL}/conflicts`} exact>
        <div id="tabpanel-conflicts" role="tabpanel" aria-labelledby="tab-conflicts">
          <MergeConflicts repository={repository} pullRequest={pullRequest} mergePreventReasons={mergePreventReasons} />
        </div>
      </Route>
    );

    routePrChanges = (
      <Route path={`${baseURL}/changes`} exact>
        <div id="tabpanel-conflicts" role="tabpanel" aria-labelledby="tab-conflicts">
          <RootChangesContainer pullRequest={pullRequest} repository={repository} />
        </div>
      </Route>
    );
  }

  return (
    <Switch>
      <Redirect from={baseURL} to={urls.concat(baseURL, pullRequest ? "comments" : "changesets")} exact />
      {routeComments}
      {routeChangeset}
      {routeChangesetPagination}
      {routeDiff}
      {routeConflicts}
      {routePrChanges}
    </Switch>
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
  const location = useLocation();
  const match = useRouteMatch();
  const baseURL = match.url;

  const activeTab = (suffix: string) => {
    return isUrlSuffixMatching(baseURL, location.pathname, suffix);
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
      <Routes
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
