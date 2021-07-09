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
import React, { FC, useEffect, useRef } from "react";
import { Comment, PullRequest } from "../types/PullRequest";

import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { useLocation } from "react-router-dom";
import { Link, Repository } from "@scm-manager/ui-types";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import styled from "styled-components";
import { usePullRequestComments } from "../pullRequest";

const COMMENT_URL_HASH_REGEX = /^#comment-(.*)$/;

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const CommentWrapper = styled.div`
  border-top: 1px solid #dbdbdb;

  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

const RootCommentContainer: FC<Props> = ({ repository, pullRequest }) => {
  const location = useLocation();
  const { data: comments, error, isLoading } = usePullRequestComments(repository, pullRequest);
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isLoading && contentRef.current) {
      setTimeout(() => {
        const match = location.hash.match(COMMENT_URL_HASH_REGEX);
        if (match) {
          const anchorId = `#comment-${match[1]}`;
          const element = contentRef.current?.querySelector(anchorId);
          if (element) {
            element.scrollIntoView();
          }
        }
      });
    }
  }, [isLoading, contentRef]);

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (isLoading) {
    return <Loading />;
  }
  if (!comments) {
    return <div />;
  }

  const createLink = (comments?._links?.create as Link)?.href || undefined;
  return (
    <div ref={contentRef}>
      {comments._embedded.pullRequestComments?.map((rootComment: Comment) => (
        <CommentWrapper key={rootComment.id} className="comment-wrapper">
          <PullRequestComment
            repository={repository}
            pullRequest={pullRequest}
            comment={rootComment}
            createLink={createLink}
          />
        </CommentWrapper>
      ))}
      {createLink && <CreateComment repository={repository} pullRequest={pullRequest} url={createLink} />}
    </div>
  );
};

export default RootCommentContainer;
