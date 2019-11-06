import React from "react";
import styled from "styled-components";
import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Link, Links } from "@scm-manager/ui-types";
import { Comment, Comments, PullRequest, Reply } from "../types/PullRequest";
import { getPullRequestComments } from "../pullRequest";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import produce from "immer";

const CommentWrapper = styled.div`
  border-top: 1px solid #dbdbdb;

  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

type Props = {
  pullRequest: PullRequest;
};

type State = {
  comments: Comment[];
  links?: Links;
  error?: Error;
  replyEditor?: Reply;
  loading: boolean;
};

class PullRequestComments extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      comments: []
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
      error
    });
  };

  updatePullRequestComments = () => {
    this.setState({
      loading: true
    });
    const url = (this.props.pullRequest._links.comments as Link).href;
    getPullRequestComments(url)
      .then((commentResponse: Comments) => {
        let comments = [];
        if (commentResponse && commentResponse._embedded) {
          comments = commentResponse._embedded.pullRequestComments;
        }
        this.setState({
          loading: false,
          comments,
          links: commentResponse._links
        });
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  appendComment = (comment: Comment) => {
    this.setState((state: Readonly<State>) => {
      return produce(state, draft => {
        draft.comments.push(comment);
      });
    });
  };

  deleteComment = (comment: Comment) => {
    return this.setState(state => {
      return produce(state, draft => {
        const index = state.comments.findIndex(c => c.id === comment.id);
        if (index >= 0) {
          draft.comments.splice(index, 1);
        }
      });
    });
  };

  replaceComment = (comment: Comment) => {
    return this.setState(state => {
      return produce(state, draft => {
        const index = state.comments.findIndex(c => c.id === comment.id);
        if (index >= 0) {
          draft.comments[index] = comment;
        }
      });
    });
  };

  render() {
    const { loading, error, comments, links } = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (loading) {
      return <Loading />;
    }
    if (!links) {
      return <div />;
    }

    const createLink = links.create ? (links.create as Link).href : undefined;
    return (
      <>
        {comments.map(rootComment => (
          <CommentWrapper className="comment-wrapper">
            <PullRequestComment
              comment={rootComment}
              onDelete={this.deleteComment}
              onUpdate={this.replaceComment}
              refresh={this.updatePullRequestComments}
              createLink={createLink}
              handleError={this.handleError}
            />
          </CommentWrapper>
        ))}
        {createLink && (
          <CreateComment url={createLink} onCreation={this.appendComment} refresh={this.updatePullRequestComments} />
        )}
      </>
    );
  }
}

export default PullRequestComments;
