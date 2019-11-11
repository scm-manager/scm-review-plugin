import React, {FC, useState, useReducer, useEffect} from "react";
import reducer, {createComment, fetchAll, initialState} from "./module";
import {Comments, PullRequest} from "../types/PullRequest";
import {getPullRequestComments} from "../pullRequest";

import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Link  } from "@scm-manager/ui-types";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import styled from "styled-components";

type Props = {
  pullRequest: PullRequest;
};

const CommentWrapper = styled.div`
  border-top: 1px solid #dbdbdb;

  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

const RootCommentContainer: FC<Props> = ({pullRequest}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [links, setLinks] = useState();
  const [comments, dispatch] = useReducer(reducer, initialState);

  useEffect(() => {
    const url = (pullRequest._links.comments as Link).href;
    getPullRequestComments(url)
      .then((commentResponse: Comments) => {
        let comments = [];
        if (commentResponse && commentResponse._embedded) {
          comments = commentResponse._embedded.pullRequestComments;
        }
        setLinks(commentResponse._links);
        setLoading(false);
        dispatch(fetchAll(comments));
      })
      .catch(error => {
        setLoading(false);
        setError(error);
      });
  }, [pullRequest]);

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
          <PullRequestComment
            comment={rootComment}
            dispatch={dispatch}
            createLink={createLink}
            handleError={err => setError(err)}
          />
        </CommentWrapper>
      ))}
      {createLink && (
        <CreateComment url={createLink} onCreation={c => dispatch(createComment(c))} />
      )}
    </>
  );

};

export default RootCommentContainer;
