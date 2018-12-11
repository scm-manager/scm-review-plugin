// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { translate } from "react-i18next";
import Changesets from "./Changesets";
import {Route, Link, withRouter} from "react-router-dom";
import Diff from "./Diff";

type Props = {
  repository: Repository,
  source: string,
  target: string,
  baseURL: string,

  // context props
  location: any,
  t: string => string
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

  constructor(props: Props) {
    super(props);
  }

  navigationClass(suffix: string) {
    const { baseURL, location } = this.props;
    if (isUrlSuffixMatching(baseURL, location.pathname, suffix)) {
      return "is-active";
    }
    return "";
  }

  render() {
    const { repository, source, target, baseURL } = this.props;

    return (
      <>
        <div className="tabs">
          <ul>
            <li className={ this.navigationClass("changesets") }>
              <Link to={`${baseURL}/changesets/`}>Commits</Link>
            </li>
            <li className={ this.navigationClass("diff") }>
              <Link to={`${baseURL}/diff/`}>Diff</Link>
            </li>
          </ul>
        </div>

        <Route
          path={`${baseURL}/changesets`}
          render={() => <Changesets repository={repository} source={source} target={target} />}
          exact
        />
        <Route
          path={`${baseURL}/changesets/:page`}
          render={() => <Changesets repository={repository} source={source} target={target} />}
          exact
        />
        <Route
          path={`${baseURL}/diff`}
          render={() => <Diff repository={repository} source={source} target={target} />}
        />
      </>
    );
  }
}

export default withRouter(translate("plugins")(PullRequestInformation));
