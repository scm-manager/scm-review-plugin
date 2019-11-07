import React from "react";
import PullRequestComment from "./PullRequestComment";
import { Comment } from "../types/PullRequest";

type Props = {
  comment: Comment;
  refresh?: () => void;
  handleError: (error: Error) => void;
  child: boolean;
};

class RecursivePullRequestComment extends React.Component<Props> {
  render() {
    return (
      <PullRequestComment
        comment={this.props.comment}
        refresh={this.props.refresh}
        handleError={this.props.handleError}
        child={this.props.child}
      />
    );
  }
}

export default RecursivePullRequestComment;
