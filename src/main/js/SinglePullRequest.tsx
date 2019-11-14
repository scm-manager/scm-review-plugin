import React from "react";
import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Switch, Route, withRouter, RouteComponentProps } from "react-router-dom";
import PullRequestDetails from "./PullRequestDetails";
import { Repository, Link } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { getPullRequest } from "./pullRequest";
import { History } from "history";
import Edit from "./Edit";

type Props = RouteComponentProps & {
  repository: Repository;
  userAutocompleteLink: string;
};

type State = {
  pullRequest?: PullRequest;
  error?: Error;
  loading: boolean;
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false
    };
  }

  componentDidMount(): void {
    this.fetchPullRequest();
  }

  fetchPullRequest = (): void => {
    const { repository } = this.props;
    const pullRequestNumber = this.props.match.params.pullRequestNumber;
    const url = (repository._links.pullRequest as Link).href + "/" + pullRequestNumber;
    getPullRequest(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequest: response,
          loading: false
        });
      }
    });
  };

  onChangePullRequest = (changes: Partial<PullRequest>) => {
    const { pullRequest } = this.state;
    this.setState({ pullRequest: {...pullRequest, ...changes}})
  };

  render() {
    const { match, repository, userAutocompleteLink } = this.props;
    const { loading, error, pullRequest } = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!pullRequest || loading) {
      return <Loading />;
    }

    return (
      <Switch>
        <Route
          component={() => (
            <Edit repository={repository} pullRequest={pullRequest} userAutocompleteLink={userAutocompleteLink} onChangePullRequest={this.onChangePullRequest}/>
          )}
          path={`${match.url}/edit`}
          exact
        />
        <Route
          component={() => <PullRequestDetails repository={repository} pullRequest={pullRequest} onChangePullRequest={this.onChangePullRequest}/>}
          path={`${match.url}`}
        />
      </Switch>
    );
  }
}

export default withRouter(SinglePullRequest);
