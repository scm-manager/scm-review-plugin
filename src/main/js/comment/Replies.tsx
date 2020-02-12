import React, { Dispatch, FC } from "react";
import { Comment } from "../types/PullRequest";
import PullRequestComment from "./PullRequestComment";

type Props = {
  comment: Comment;
  createLink?: string;
  dispatch: Dispatch<any>;
};

const Replies: FC<Props> = ({ comment, createLink, dispatch }) => {
  if (!comment._embedded || !comment._embedded.replies) {
    return null;
  }

  const replies = comment._embedded.replies;
  return (
    <>
      {replies.map((reply: Comment) => (
        <PullRequestComment
          key={reply.id}
          parent={comment}
          createLink={createLink}
          comment={reply}
          dispatch={dispatch}
        />
      ))}
    </>
  );
};

export default Replies;
