import React, { FC, useReducer } from "react";
import reducer, { initialState } from "./reducer";
import { createComment } from "./actiontypes";

import { PullRequest } from "../types/PullRequest";

import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import styled from "styled-components";
import useComments from "./useComments";

type Props = {
  pullRequest: PullRequest;
};

const CommentWrapper = styled.div`
  border-top: 1px solid #dbdbdb;

  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

const RootCommentContainer: FC<Props> = ({ pullRequest }) => {
  const [comments, dispatch] = useReducer(reducer, initialState);
  const { error, loading, links } = useComments(pullRequest, dispatch);

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
        <CommentWrapper key={rootComment.id} className="comment-wrapper">
          <PullRequestComment comment={rootComment} dispatch={dispatch} createLink={createLink} />
        </CommentWrapper>
      ))}
      {createLink && <CreateComment url={createLink} onCreation={c => dispatch(createComment(c))} />}
    </>
  );
};

export default RootCommentContainer;
