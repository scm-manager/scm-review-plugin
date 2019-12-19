import React from "react";
import { Repository } from "@scm-manager/ui-types";
import { urls } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, RouteComponentProps, Switch, withRouter } from "react-router-dom";
import RootComments from "./comment/RootComments";
import { PullRequest } from "./types/PullRequest";
import DiffRoute from "./diff/DiffRoute";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    baseURL: string;
    source: string;
    target: string;
    status: string;
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
    const { pullRequest, repository, baseURL, status, target, source, t } = this.props;

    let changesetTab = null;
    let diffTab = null;
    let routeChangeset = null;
    let routeChangesetPagination = null;
    let routeDiff = null;

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
          render={() => <Changesets repository={repository} source={sourceRevision} target={targetRevision} />}
          exact
        />
      );
      routeChangesetPagination = (
        <Route
          path={`${baseURL}/changesets/:page`}
          render={() => <Changesets repository={repository} source={sourceRevision} target={targetRevision} />}
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
    }
    const routes = (
      <Switch>
        <Redirect from={baseURL} to={urls.concat(baseURL, pullRequest ? "comments" : "changesets")} exact />
        <Route path={`${baseURL}/comments`} render={() => <RootComments pullRequest={pullRequest} />} exact />
        {routeChangeset}
        {routeChangesetPagination}
        {routeDiff}
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
          </ul>
        </div>
        {routes}
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestInformation));
