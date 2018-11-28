// @flow
import React from "react";
import {
  ErrorNotification, SubmitButton, Subtitle, Title
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";

type Props = {
  repository: Repository,
  classes: any,
  t: string => string,
  match: any
};

type State = {
  pullRequestNumber: number
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequestNumber : this.props.match.params.pullRequestNumber
    };
  }

  render() {
    const {repository, t} = this.props;
    const { pullRequestNumber } = this.state;
    return (
      <div className="columns">
        <div className="column">
          <Title title={t("scm-review-plugin.create.title") + pullRequestNumber} />
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

        </div>
      </div>
    );
  }
}

export default withRouter(translate("plugins")(SinglePullRequest));
