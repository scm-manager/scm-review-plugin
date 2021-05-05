/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import { ErrorNotification, Loading, Notification } from "@scm-manager/ui-components";
import { Route, RouteComponentProps, Switch, withRouter } from "react-router-dom";
import PullRequestDetails from "./PullRequestDetails";
import { Link, Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { getPullRequest, getReviewer } from "./pullRequest";
import Edit from "./Edit";
import { withTranslation, WithTranslation } from "react-i18next";

type Props = RouteComponentProps &
  WithTranslation & {
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
    if (this.props.repository._links.pullRequest) {
      this.fetchPullRequest();
    }
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
    const { match, repository, userAutocompleteLink, t } = this.props;
    const { loading, error, pullRequest } = this.state;

    if (!repository._links.pullRequest) {
      return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
    }

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!pullRequest || loading) {
      return <Loading />;
    }

    return (
      <Switch>
        <Route path={`${match.url}/edit`} exact>
          <Edit
            repository={repository}
            pullRequest={pullRequest}
            userAutocompleteLink={userAutocompleteLink}
            fetchPullRequest={this.fetchPullRequest}
          />
        </Route>
        <Route path={`${match.url}`}>
          <PullRequestDetails
            repository={repository}
            pullRequest={pullRequest}
            fetchReviewer={this.fetchReviewer}
            fetchPullRequest={this.fetchPullRequest}
          />
        </Route>
      </Switch>
    );
  }
}

export default withRouter(withTranslation("plugins")(SinglePullRequest));
