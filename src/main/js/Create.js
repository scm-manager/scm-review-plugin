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
import type { BasicPullRequest } from "./types/PullRequest";
import { createPullRequest } from "./pullRequest";
import {Trans, translate} from "react-i18next";
import PullRequestInformation from "./PullRequestInformation";
import type { History } from "history";
import {withRouter} from "react-router-dom";

const styles = {
  controlButtons: {
    paddingTop: "1.5em"
  }
};

type Props = {
  repository: Repository,
  classes: any,
  match: any,
  t: string => string,
  history: History
};

type State = {
  pullRequest?: BasicPullRequest,
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
    const { history, repository } = this.props;
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-requests`);
  };

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    this.setState({ loading: true });

    createPullRequest(repository._links.pullRequest.href, pullRequest).then(
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

  verify = (pullRequest: BasicPullRequest) => {
    const { source, target, title } = pullRequest;
    if (source && target && title) {
      return source !== target;
    }
    return false;
  };

  handleFormChange = (pullRequest: BasicPullRequest) => {
    this.setState({
      pullRequest,
      disabled: !this.verify(pullRequest)
    });
  };

  render() {
    const { repository, classes, match, t } = this.props;
    const { pullRequest, loading, error, disabled } = this.state;

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    let information = null;
    if ( pullRequest ) {
      information = <PullRequestInformation repository={repository}
                              pullRequest={pullRequest}
                              baseURL={match.url}/>;
    }
    let subtitle = (<Trans i18nKey="scm-review-plugin.create.subtitle"  values={{ repositoryName: repository.name }}  /> );

    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.create.title")} />
          <Subtitle
            subtitle={subtitle}
          />

          {notification}

          <CreateForm
            repository={repository}
            onChange={this.handleFormChange}
          />

          {information}

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

export default withRouter(injectSheet(styles)(translate("plugins")(Create)));
