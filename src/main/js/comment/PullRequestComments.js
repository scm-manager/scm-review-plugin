// @flow
import React from "react";
import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import type {Comments, PullRequest, Reply} from "../types/PullRequest";
import { getPullRequestComments } from "../pullRequest";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";

type Props = {
  pullRequest: PullRequest
};

type State = {
  pullRequestComments?: Comments,
  error?: Error,
  replyEditor: Reply,
  loading: boolean
};

class PullRequestComments extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    if (pullRequest && pullRequest._links && pullRequest._links.comments) {
      this.updatePullRequestComments();
    } else {
      this.setState({
        loading: false
      });
    }
  }

  handleError = (error: Error) => {
    this.setState({
      loading: false,
      error: error
    });
  };

  updatePullRequestComments = () => {
    this.setState({
      loading: true
    });
    const url = this.props.pullRequest._links.comments.href;
    getPullRequestComments(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequestComments: response,
          loading: false
        });
      }
    });
  };

  render() {
    const { loading, error, pullRequestComments } = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (loading) {
      return <Loading />;
    }
    if (!pullRequestComments) {
      return <div />;
    }

    if (
      pullRequestComments &&
      pullRequestComments._embedded &&
      pullRequestComments._embedded.pullRequestComments
    ) {
      const comments = pullRequestComments._embedded.pullRequestComments;

      const createLink = pullRequestComments._links.create
        ? pullRequestComments._links.create.href
        : null;
      return (
        <>
          {comments.map(rootComment => (
            <div className="comment-wrapper">
                <PullRequestComment
                  comment={rootComment}
                  refresh={this.updatePullRequestComments}
                  handleError={this.onError}
                />
            </div>
          ))}
          {createLink ? (
            <CreateComment
              url={createLink}
              refresh={this.updatePullRequestComments}
              handleError={this.handleError}
            />
          ) : (
            ""
          )}
        </>
      );
    }
  }
}

export default PullRequestComments;
