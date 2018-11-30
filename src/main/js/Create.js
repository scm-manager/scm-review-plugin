// @flow
import React from "react";
import {
  Title,
  Subtitle,
  SubmitButton,
  ErrorNotification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import injectSheet from "react-jss";
import type { PullRequest } from "./PullRequest";
import { createPullRequest } from "./pullRequest";
import { translate } from "react-i18next";

const styles = {
  controlButtons: {
    paddingTop: "1.5em"
  }
};

type Props = {
  repository: Repository,
  classes: any,
  t: string => string
};

type State = {
  pullRequest?: PullRequest,
  loading: boolean,
  error?: Error,
  disabled: boolean
};

class Create extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      disabled: true
    };
  }

  pullRequestCreated = () => {
    //TODO: if overview pull request page exists - open that!
  };

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    this.setState({ loading: true });

    createPullRequest(repository._links.newPullRequest.href, pullRequest).then(
      result => {
        if (result.error) {
          this.setState({ loading: false, error: result.error });
        } else {
          this.setState({ loading: false });
          this.pullRequestCreated();
        }
      }
    );
  };

  verify = (pullRequest: PullRequest) => {
    const { source, target, title } = pullRequest;
    if (source && target && title) {
      return source !== target;
    }
    return false;
  };

  handleFormChange = (pullRequest: PullRequest) => {
    this.setState({
      pullRequest,
      disabled: !this.verify(pullRequest)
    });
  };

  render() {
    const { repository, classes, t } = this.props;
    const { loading, error, disabled } = this.state;

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    return (
      <div className="columns">
        <div className="column is-clipped">
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

export default injectSheet(styles)(translate("plugins")(Create));
