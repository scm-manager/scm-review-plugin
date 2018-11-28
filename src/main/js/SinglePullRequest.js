// @flow
import React from "react";
import {
  ErrorNotification, SubmitButton, Subtitle, Title
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";

type Props = {
  repository: Repository,
  classes: any,
  t: string => string
};

type State = {

};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {

    };
  }

  render() {
    const {t} = this.props;
    return (
      <div className="columns">
        <div className="column">
          <Title title={t("scm-review-plugin.create.title")} />
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

export default (translate("plugins")(SinglePullRequest));
