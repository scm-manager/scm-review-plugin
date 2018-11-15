//@flow
import React from "react";
import {
  Title,
  Subtitle,
  SubmitButton,
  ErrorNotification,
  Notification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import injectSheet from "react-jss";
import type {PullRequest} from "./PullRequest";
import {createPullRequest} from "./pullRequest";

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
  disabled: boolean,
  success?: boolean
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

    createPullRequest(repository._links.newPullRequest.href, pullRequest)
      .then( result => {
        if(result.error){
          this.setState({ loading: false, error: result.error });
        }
        else {
          this.setState({ loading: false, success: true });
        }
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
    const { loading, error, disabled, success } = this.state;

    let notification = null;
    if(error) {
      notification = <ErrorNotification error={error} />
    } 
    else if(success){
     notification = <Notification type={"success"} children={"added new pull request successful"}/>
    }

    return (
      <div className="columns">
        <div className="column">
          <Title title="Pull Request" />
          <Subtitle
            subtitle={"Creates a new Pull Request for " + repository.name}
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
