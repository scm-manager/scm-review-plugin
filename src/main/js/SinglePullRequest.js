// @flow
import React from "react";
import {
  ErrorNotification, SubmitButton, Subtitle, Title
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import CreateForm from "./CreateForm";

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
    return (
      <div className="columns">
        <div className="column">
          <Title title={t("scm-review-plugin.create.title")} />
          <Subtitle
            subtitle={t("scm-review-plugin.create.subtitle") + repository.name}
          />
          {notification}
          <CreateForm
            repository={repository}
            onChange={this.handleFormChange}
          />

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

          <div className={classes.controlButtons}>
            <SubmitButton
              label={t("scm-review-plugin.create.submitButton")}
              action={this.submit}
              loading={loading}
              disabled={disabled}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default (translate("plugins")(SinglePullRequest));
