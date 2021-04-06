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
import React, { FC, useEffect, useReducer, useRef } from "react";
import reducer, { initialState } from "./reducer";
import { createComment } from "./actiontypes";

import { PullRequest } from "../types/PullRequest";

import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { useLocation } from "react-router-dom";
import { Link } from "@scm-manager/ui-types";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import styled from "styled-components";
import useComments from "./useComments";

const COMMENT_URL_HASH_REGEX = /^#comment-(.*)$/;

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
  const location = useLocation();
  const [comments, dispatch] = useReducer(reducer, initialState);
  const { error, loading, links } = useComments(pullRequest, dispatch);
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!loading && contentRef.current) {
      setTimeout(() => {
        const match = location.hash.match(COMMENT_URL_HASH_REGEX);
        if (match) {
          const anchorId = `#comment-${match[1]}`;
          const element = contentRef.current?.querySelector(anchorId);
          if (element) {
            element.scrollIntoView();
          }
        }
      })
    }
  }, [loading, contentRef]);

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
    <div ref={contentRef}>
      {comments.map(rootComment => (
        <CommentWrapper key={rootComment.id} className="comment-wrapper">
          <PullRequestComment comment={rootComment} dispatch={dispatch} createLink={createLink} />
        </CommentWrapper>
      ))}
      {createLink && <CreateComment url={createLink} onCreation={c => dispatch(createComment(c))} />}
    </div>
  );
};

export default RootCommentContainer;
