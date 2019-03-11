// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { urls } from "@scm-manager/ui-components";
import { translate } from "react-i18next";
import Changesets from "./Changesets";
import { Link, Redirect, Route, Switch, withRouter } from "react-router-dom";
import Diff from "./diff/Diff";
import PullRequestComments from "./comment/PullRequestComments";
import type { PullRequest } from "./types/PullRequest";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  baseURL: string,
  source: string,
  target: string,
  status: string,

  // context props
  location: any,
  t: string => string
};

export function isUrlSuffixMatching(
  baseURL: string,
  url: string,
  suffix: string
) {
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
    const {
      pullRequest,
      repository,
      baseURL,
      status,
      target,
      source
    } = this.props;

    let changesetTab = null;
    let diffTab = null;
    let routes = null;
    let routeChangeset = null;
    let routeDiff = null;

    if (status && status === "OPEN") {
      changesetTab = (
        <li className={this.navigationClass("changesets")}>
          <Link to={`${baseURL}/changesets/`}>Commits</Link>
        </li>
      );
      routeChangeset = (
        <>
          <Route
            path={`${baseURL}/changesets`}
            render={() => (
              <Changesets
                repository={repository}
                source={source}
                target={target}
              />
            )}
            exact
          />
          <Route
            path={`${baseURL}/changesets/:page`}
            render={() => (
              <Changesets
                repository={repository}
                source={source}
                target={target}
              />
            )}
            exact
          />
        </>
      );
      routeDiff = (
        <Route
          path={`${baseURL}/diff`}
          render={() => (
            <Diff
              repository={repository}
              pullRequest={pullRequest}
              source={source}
              target={target}
            />
          )}
        />
      );
      diffTab = (
        <li className={this.navigationClass("diff")}>
          <Link to={`${baseURL}/diff/`}>Diff</Link>
        </li>
      );
    }
    routes = (
      <Switch>
        <Redirect
          from={baseURL}
          to={urls.concat(baseURL, pullRequest ? "comments" : "changesets")}
          exact
        />
        {routeChangeset}
        {routeDiff}
        <Route
          path={`${baseURL}/comments`}
          render={() => <PullRequestComments pullRequest={pullRequest} />}
          exact
        />
      </Switch>
    );

    let commentTab = pullRequest ? (
      <li className={this.navigationClass("comments")}>
        <Link to={`${baseURL}/comments/`}>Comments</Link>
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

export default withRouter(translate("plugins")(PullRequestInformation));
