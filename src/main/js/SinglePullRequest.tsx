import React from "react";
import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Route, RouteComponentProps, Switch, withRouter } from "react-router-dom";
import PullRequestDetails from "./PullRequestDetails";
import { Link, Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { getPullRequest, getReviewer } from "./pullRequest";
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

  shouldComponentUpdate(nextProps: Readonly<Props>, nextState: Readonly<State>, nextContext: any): boolean {
    return nextState.pullRequest !== this.state.pullRequest;
  }

  fetchPullRequest = () => {
    const { repository } = this.props;
    const pullRequestNumber = this.props.match.params.pullRequestNumber;
    const url = (repository._links.pullRequest as Link).href + "/" + pullRequestNumber;
    getPullRequest(url)
      .then(pullRequest => {
        this.setState({
          pullRequest,
          loading: false
        });
      })
      .catch(error => {
        this.setState({
          error,
          loading: false
        });
      });
  };

  fetchReviewer = (): void => {
    const { pullRequest } = this.state;
    if (pullRequest && pullRequest._links && pullRequest._links.self && (pullRequest._links.self as Link).href) {
      const url = (pullRequest._links.self as Link).href + "?fields=reviewer&fields=_links";
      getReviewer(url).then(response => {
        if (response.error) {
          this.setState({
            error: response.error
          });
        } else {
          this.setState({ pullRequest: { ...pullRequest, reviewer: response.reviewer, _links: response._links } });
        }
      });
    }
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
            <Edit
              repository={repository}
              pullRequest={pullRequest}
              userAutocompleteLink={userAutocompleteLink}
              fetchPullRequest={this.fetchPullRequest}
            />
          )}
          path={`${match.url}/edit`}
          exact
        />
        <Route
          component={() => (
            <PullRequestDetails
              repository={repository}
              pullRequest={pullRequest}
              fetchReviewer={this.fetchReviewer}
              fetchPullRequest={this.fetchPullRequest}
            />
          )}
          path={`${match.url}`}
        />
      </Switch>
    );
  }
}

export default withRouter(SinglePullRequest);
