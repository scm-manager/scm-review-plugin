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
import React, { FC } from "react";
import { Repository } from "@scm-manager/ui-types";
import { Icon, urls } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, Switch, useLocation, useRouteMatch } from "react-router-dom";
import RootComments from "./comment/RootCommentContainer";
import { PullRequest } from "./types/PullRequest";
import DiffRoute from "./diff/DiffRoute";
import MergeConflicts from "./MergeConflicts";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
  status?: string;
  mergeHasNoConflict: boolean;
  targetBranchDeleted: boolean;
  shouldFetchChangesets?: boolean;
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

  return (
    <div className="tabs">
      <ul role="tablist">
        {commentTab}
        {changesetTab}
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
  source: string;
  target: string;
  shouldFetchChangesets?: boolean;
};

const Routes: FC<RouteProps> = ({
  repository,
  source,
  target,
  isClosed,
  pullRequest,
  baseURL,
  mergeHasNoConflict,
  shouldFetchChangesets
}) => {
  let routeComments = null;
  let routeChangeset = null;
  let routeChangesetPagination = null;
  let routeDiff = null;
  let routeConflicts = null;
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
          />
        </div>
      </Route>
    );
    routeConflicts = !mergeHasNoConflict && (
      <Route path={`${baseURL}/conflicts`} exact>
        <div id="tabpanel-conflicts" role="tabpanel" aria-labelledby="tab-conflicts">
          <MergeConflicts repository={repository} pullRequest={pullRequest} />
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
    </Switch>
  );
};

const PullRequestInformation: FC<Props> = ({
  pullRequest,
  mergeHasNoConflict,
  status,
  targetBranchDeleted,
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
        baseURL={baseURL}
      />
    </>
  );
};

export default PullRequestInformation;
