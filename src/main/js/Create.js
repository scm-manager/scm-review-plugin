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
  pullRequest?: Object, // TODO add type
  loading: boolean,
  error?: Error
};

class Create extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false
    };
  }

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    //this.setState({loading: true});
    // TODO handle loading, success and error
    apiClient.post(repository._links.newPullRequest.href, pullRequest)
      .catch(cause => {
        const error = new Error(`could not fetch users: ${cause.message}`);
        this.setState({error: error});
      });;
  };

  handleFormChange = (pullRequest) => {
    this.setState({
      pullRequest
    });
  };

  render() {
    const {repository, classes} = this.props;
    const {loading, error} = this.state;
  console.log(this.state.pullRequest);

  if(error)
    return <ErrorNotification error={error}/>;

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
            <SubmitButton label="Submit" action={this.submit} loading={loading}/>
          </div>
        </div>
      </div>
    );
  }

}

export default injectSheet(styles)(Create);
