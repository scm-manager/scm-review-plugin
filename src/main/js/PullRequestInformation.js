// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { translate } from "react-i18next";

type Props = {
  repository: Repository,
  t: string => string
};

class PullRequestInformation extends React.Component<Props> {


  render() {
    return (
      <>
          <div className="tabs">
            <ul>
              <li className="is-active">
                <a>Commits</a>
              </li>
              <li>
                <a>Diff</a>
              </li>
            </ul>
          </div>

          <p>The Changelog ...</p>
        </>
    );
  }
}

export default (translate("plugins")(PullRequestInformation));
