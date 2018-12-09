// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { translate } from "react-i18next";
import type { PullRequest } from "./types/PullRequest";
import PullRequestComment from "./PullRequestComment";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  t: string => string
};

class PullRequestInformation extends React.Component<Props> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    const { pullRequest, repository} = this.props;
    return (
      <>
          <div className="tabs">
            <ul>
              <li className="is-active">
                <a>Comments</a>
              </li>

              <li >
                <a>Commits</a>
              </li>

              <li>
                <a>Diff</a>
              </li>
            </ul>
          </div>

        <PullRequestComment pullRequest={pullRequest} repository={repository}/>
        </>
    );
  }
}

export default (translate("plugins")(PullRequestInformation));
