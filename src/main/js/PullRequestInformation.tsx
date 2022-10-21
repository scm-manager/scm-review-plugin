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
import { Icon, urls, createAttributesForTesting } from "@scm-manager/ui-components";
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

type TabProps = {
  isClosed: boolean;
  baseURL: string;
  navigationClass: (type: string) => string;
  pullRequest: PullRequest;
  targetBranchDeleted: boolean;
  mergeHasNoConflict: boolean;
};

const Tabs: FC<TabProps> = ({
  isClosed,
  pullRequest,
  navigationClass,
  baseURL,
  targetBranchDeleted,
  mergeHasNoConflict
}) => {
  const [t] = useTranslation("plugins");
  let changesetTab = null;
  let diffTab = null;
  let conflictsTab = null;

  if (!isClosed || (pullRequest.sourceRevision && pullRequest.targetRevision)) {
    changesetTab = (
      <li className={navigationClass("changesets")} {...createAttributesForTesting("changesets")}>
        <Link to={`${baseURL}/changesets/`}>{t("scm-review-plugin.pullRequest.tabs.commits")}</Link>
      </li>
    );

    diffTab = (
      <li className={navigationClass("diff")} {...createAttributesForTesting("diff")}>
        <Link to={`${baseURL}/diff/`}>{t("scm-review-plugin.pullRequest.tabs.diff")}</Link>
      </li>
    );

    conflictsTab = !mergeHasNoConflict && (
      <li className={navigationClass("conflicts")} {...createAttributesForTesting("conflicts")}>
        <Link to={`${baseURL}/conflicts/`}>
          {t("scm-review-plugin.pullRequest.tabs.conflicts")} &nbsp;{" "}
          <Icon color={"warning"} name={"exclamation-triangle"} />
        </Link>
      </li>
    );
  }

  const commentTab = pullRequest._links?.comments ? (
    <li className={navigationClass("comments")}>
      <Link to={`${baseURL}/comments/`}>{t("scm-review-plugin.pullRequest.tabs.comments")}</Link>
    </li>
  ) : null;

  return (
    <div className="tabs">
      <ul>
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
  let routeChangeset = null;
  let routeChangesetPagination = null;
  let routeDiff = null;
  let routeConflicts = null;
  if (!isClosed || (pullRequest.sourceRevision && pullRequest.targetRevision)) {
    const sourceRevision = isClosed && pullRequest.sourceRevision ? pullRequest.sourceRevision : source;
    const targetRevision = isClosed && pullRequest.targetRevision ? pullRequest.targetRevision : target;
    routeChangeset = (
      <Route path={`${baseURL}/changesets`} exact>
        <Changesets repository={repository} pullRequest={pullRequest} shouldFetchChangesets={shouldFetchChangesets} />
      </Route>
    );
    routeChangesetPagination = (
      <Route path={`${baseURL}/changesets/:page`} exact>
        <Changesets repository={repository} pullRequest={pullRequest} shouldFetchChangesets={shouldFetchChangesets} />
      </Route>
    );
    routeDiff = (
      <Route path={`${baseURL}/diff`} exact>
        <DiffRoute repository={repository} pullRequest={pullRequest} source={sourceRevision} target={targetRevision} />
      </Route>
    );

    routeConflicts = !mergeHasNoConflict && (
      <Route path={`${baseURL}/conflicts`} exact>
        <MergeConflicts repository={repository} pullRequest={pullRequest} />
      </Route>
    );
  }
  return (
    <Switch>
      <Redirect from={baseURL} to={urls.concat(baseURL, pullRequest ? "comments" : "changesets")} exact />
      <Route path={`${baseURL}/comments`} exact>
        {pullRequest ? <RootComments repository={repository} pullRequest={pullRequest} /> : null}
      </Route>
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

  const navigationClass = (suffix: string) => {
    if (isUrlSuffixMatching(baseURL, location.pathname, suffix)) {
      return "is-active";
    }
    return "";
  };

  const isClosedPullRequest = status === "MERGED" || status === "REJECTED";

  return (
    <>
      <Tabs
        isClosed={isClosedPullRequest}
        baseURL={baseURL}
        navigationClass={navigationClass}
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
