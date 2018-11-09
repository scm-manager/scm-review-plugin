//@flow
import React from "react";
import {apiClient, Title, Subtitle, SubmitButton} from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import CreateForm from './CreateForm';
import injectSheet from "react-jss";

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
  pullRequest?: Object // TODO add type
};

class Create extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {};
  }

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    // TODO handle loading, success and error
    apiClient.post(repository._links.newPullRequest.href, pullRequest);
  };

  handleFormChange = (pullRequest) => {
    this.setState({
      pullRequest
    });
  };

  render() {
    const {repository, classes} = this.props;

    return (
      <div className="columns">
        <div className="column">

          <Title title="Pull Request"/>
          <Subtitle subtitle={"Creates a new Pull Request for " + repository.name}/>

          <CreateForm repository={repository} onChange={this.handleFormChange}/>

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

          <p>
            The Changelog ...
          </p>

          <div className={classes.controlButtons}>
            <SubmitButton label="Submit" action={this.submit} />
          </div>
        </div>
      </div>
    );
  }

}

export default injectSheet(styles)(Create);
