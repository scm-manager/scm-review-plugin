import React from "react";
import { Repository } from "@scm-manager/ui-types";
import { urls, Icon } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, Switch, withRouter, RouteComponentProps } from "react-router-dom";
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
  navigationClass(suffix: string) {
    const { baseURL, location } = this.props;
    if (isUrlSuffixMatching(baseURL, location.pathname, suffix)) {
      return "is-active";
    }
    return "";
  }

  render() {
    const { pullRequest, repository, baseURL, status, target, source, mergeHasNoConflict, t } = this.props;

    let changesetTab = null;
    let diffTab = null;
    let conflictsTab = null;
    let routeChangeset = null;
    let routeChangesetPagination = null;
    let routeDiff = null;
    let routeConflicts = null;

    if (status && status === "OPEN") {
      changesetTab = (
        <li className={this.navigationClass("changesets")}>
          <Link to={`${baseURL}/changesets/`}>{t("scm-review-plugin.pullRequest.tabs.commits")}</Link>
        </li>
      );
      routeChangeset = (
        <Route
          path={`${baseURL}/changesets`}
          render={() => <Changesets repository={repository} source={source} target={target} />}
          exact
        />
      );
      routeChangesetPagination = (
        <Route
          path={`${baseURL}/changesets/:page`}
          render={() => <Changesets repository={repository} source={source} target={target} />}
          exact
        />
      );
      routeDiff = (
        <Route
          path={`${baseURL}/diff`}
          render={() => <DiffRoute repository={repository} pullRequest={pullRequest} source={source} target={target} />}
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
            {diffTab}
            {conflictsTab}
          </ul>
        </div>
        {routes}
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestInformation));
