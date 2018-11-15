//@flow
import React from "react";
import {
  apiClient,
  Title,
  Subtitle,
  SubmitButton,
  ErrorNotification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import injectSheet from "react-jss";
import type {PullRequest} from "./PullRequest";

const styles = {
  controlButtons: {
    paddingTop: "1.5em"
  }
};

type Props = {
  repository: Repository,
  classes: any
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

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    this.setState({ loading: true });
    // TODO handle loading, success
    apiClient
      .post(repository._links.newPullRequest.href, pullRequest)
      .then(this.setState({ loading: false }))
      .catch(cause => {
        const error = new Error(
          `could not create pull request: ${cause.message}`
        );
        this.setState({ error: error });
      });
  };

  verify = (pullRequest: PullRequest) => {
    if(pullRequest.target && pullRequest.source && pullRequest.title){
      return true;
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
    const { repository, classes } = this.props;
    const { loading, error, disabled } = this.state;

    const errorNotification = error ? (
      <ErrorNotification error={error} />
    ) : null;

    return (
      <div className="columns">
        <div className="column">
          <Title title="Pull Request" />
          <Subtitle
            subtitle={"Creates a new Pull Request for " + repository.name}
          />
          {errorNotification}
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
              label="Submit"
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

export default injectSheet(styles)(Create);
