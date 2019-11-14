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

  /**
   * update pull request only if needed
   */
  componentDidUpdate(): void {
    const { history } = this.props;
    // the /updated path is set from the sub components after an update of the pull request.
    // this is a flag to perform fetching pull request
    if (
      history &&
      history.location.state &&
      history.location.state.from &&
      history.location.state.from.indexOf("/updated") > -1
    ) {
      this.fetchPullRequest();
      history.push();
    }
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

  onChangePullRequest = (pullRequest: PullRequest) => {
    this.setState({ pullRequest})
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
