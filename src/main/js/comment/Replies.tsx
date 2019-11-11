import React, { FC, Dispatch, useState } from "react";
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

  const onError = (err: Error) => {
    console.log(err);
  };

  return (
    <>
      {replies.map((reply: Comment) => (
        <PullRequestComment
          key={reply.id}
          parent={comment}
          createLink={createLink}
          comment={reply}
          dispatch={dispatch}
          handleError={onError}
        />
      ))}
    </>
  );
};

export default Replies;
