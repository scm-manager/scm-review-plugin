// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { translate } from "react-i18next";
import Changesets from "./Changesets";
import { Route, Link, withRouter } from "react-router-dom";
import Diff from "./Diff";
import PullRequestComments from "./comment/PullRequestComments";
import type {PullRequest} from "./types/PullRequest";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  baseURL: string,

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
    const { pullRequest, repository, baseURL } = this.props;

    let changesetTab = null;
    let diffTab = null;
    let routes = null;

    if (pullRequest.status && pullRequest.status === "OPEN") {
      changesetTab = (
        <li className={this.navigationClass("changesets")}>
          <Link to={`${baseURL}/changesets/`}>Commits</Link>
        </li>
      );
      diffTab = (
        <li className={this.navigationClass("diff")}>
          <Link to={`${baseURL}/diff/`}>Diff</Link>
        </li>
      );
      routes = (
        <>
          <Route
            path={`${baseURL}/changesets`}
            render={() => (
              <Changesets
                repository={repository}
                source={pullRequest.source}
                target={pullRequest.target}
              />
            )}
            exact
          />
          <Route
            path={`${baseURL}/changesets/:page`}
            render={() => (
              <Changesets
                repository={repository}
                source={pullRequest.source}
                target={pullRequest.target}
              />
            )}
            exact
          />
          <Route
            path={`${baseURL}/diff`}
            render={() => (
              <Diff repository={repository} source={pullRequest.source} target={pullRequest.target} />
            )}
          />
        </>
      );
    }

    let commentTab = pullRequest? (
      <li className={ this.navigationClass("comments") }>
        <Link to={`${baseURL}/comments/`}>Comments</Link>
      </li>
    ) : "";

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
        <Route
          path={`${baseURL}/comments`}
          render={() => <PullRequestComments pullRequest={pullRequest}/>}
          exact
        />
      </>
    );
  }
}

export default withRouter(translate("plugins")(PullRequestInformation));
