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
import React from "react";
import { Repository } from "@scm-manager/ui-types";
import { urls, Icon } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, RouteComponentProps, Switch, withRouter } from "react-router-dom";
import RootComments from "./comment/RootComments";
import { PullRequest } from "./types/PullRequest";
import DiffRoute from "./diff/DiffRoute";
import MergeConflicts from "./MergeConflicts";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    baseURL: string;
    source: string;
    target: string;
    status: string;
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

class PullRequestInformation extends React.Component<Props> {
  static defaultProps = {
    shouldFetchChangesets: true
  };

  navigationClass(suffix: string) {
    const { baseURL, location } = this.props;
    if (isUrlSuffixMatching(baseURL, location.pathname, suffix)) {
      return "is-active";
    }
    return "";
  }

  render() {
    const {
      pullRequest,
      repository,
      baseURL,
      status,
      target,
      source,
      mergeHasNoConflict,
      targetBranchDeleted,
      shouldFetchChangesets,
      t
    } = this.props;

    let changesetTab = null;
    let diffTab = null;
    let conflictsTab = null;
    let routeChangeset = null;
    let routeChangesetPagination = null;
    let routeDiff = null;
    let routeConflicts = null;

    const isClosedPullRequest = status === "MERGED" || status === "REJECTED";

    if (!isClosedPullRequest || (pullRequest?.sourceRevision && pullRequest?.targetRevision)) {
      changesetTab = (
        <li className={this.navigationClass("changesets")}>
          <Link to={`${baseURL}/changesets/`}>{t("scm-review-plugin.pullRequest.tabs.commits")}</Link>
        </li>
      );
      const sourceRevision = isClosedPullRequest && pullRequest?.sourceRevision ? pullRequest.sourceRevision : source;
      const targetRevision = isClosedPullRequest && pullRequest?.targetRevision ? pullRequest.targetRevision : target;
      routeChangeset = (
        <Route
          path={`${baseURL}/changesets`}
          render={() => (
            <Changesets
              repository={repository}
              source={sourceRevision}
              target={targetRevision}
              shouldFetchChangesets={shouldFetchChangesets}
            />
          )}
          exact
        />
      );
      routeChangesetPagination = (
        <Route
          path={`${baseURL}/changesets/:page`}
          render={() => (
            <Changesets
              repository={repository}
              source={sourceRevision}
              target={targetRevision}
              shouldFetchChangesets={shouldFetchChangesets}
            />
          )}
          exact
        />
      );
      routeDiff = (
        <Route
          path={`${baseURL}/diff`}
          render={() => (
            <DiffRoute
              repository={repository}
              pullRequest={pullRequest}
              source={sourceRevision}
              target={targetRevision}
            />
          )}
          exact
        />
      );
      diffTab = (
        <li className={this.navigationClass("diff")}>
          <Link to={`${baseURL}/diff/`}>{t("scm-review-plugin.pullRequest.tabs.diff")}</Link>
        </li>
      );
      routeConflicts = !mergeHasNoConflict && (
        <Route
          path={`${baseURL}/conflicts`}
          render={() => (
            <MergeConflicts repository={repository} pullRequest={pullRequest} source={source} target={target} />
          )}
          exact
        />
      );
      conflictsTab = !mergeHasNoConflict && (
        <li className={this.navigationClass("conflicts")}>
          <Link to={`${baseURL}/conflicts/`}>
            {t("scm-review-plugin.pullRequest.tabs.conflicts")} &nbsp;{" "}
            <Icon color={"warning"} name={"exclamation-triangle"} />
          </Link>
        </li>
      );
    }
    const routes = (
      <Switch>
        <Redirect from={baseURL} to={urls.concat(baseURL, pullRequest ? "comments" : "changesets")} exact />
        <Route path={`${baseURL}/comments`} render={() => <RootComments pullRequest={pullRequest} />} exact />
        {routeChangeset}
        {routeChangesetPagination}
        {routeDiff}
        {routeConflicts}
      </Switch>
    );

    const commentTab = pullRequest ? (
      <li className={this.navigationClass("comments")}>
        <Link to={`${baseURL}/comments/`}>{t("scm-review-plugin.pullRequest.tabs.comments")}</Link>
      </li>
    ) : (
      ""
    );

    return (
      <>
        <div className="tabs">
          <ul>
            {commentTab}
            {changesetTab}
            {!targetBranchDeleted && diffTab}
            {conflictsTab}
          </ul>
        </div>
        {routes}
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestInformation));
