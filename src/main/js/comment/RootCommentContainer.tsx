/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC, useEffect, useRef } from "react";
import { Comment, PullRequest } from "../types/PullRequest";

import { ErrorNotification, Loading } from "@scm-manager/ui-components";
import { useLocation } from "react-router-dom";
import { Link, Repository } from "@scm-manager/ui-types";
import PullRequestComment from "./PullRequestComment";
import CreateComment from "./CreateComment";
import styled from "styled-components";
import { useComments } from "../pullRequest";

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
  const { data: comments, error, isLoading } = useComments(repository, pullRequest);
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
  const createWithImagesLink = (comments?._links?.createWithImages as Link)?.href || undefined;
  return (
    <div ref={contentRef}>
      {comments._embedded.pullRequestComments?.map((rootComment: Comment) => (
        <CommentWrapper key={rootComment.id} className="comment-wrapper">
          <PullRequestComment
            repository={repository}
            pullRequest={pullRequest}
            comment={rootComment}
            createLink={createLink}
            createWithImageLink={createWithImagesLink}
          />
        </CommentWrapper>
      ))}
      {createLink && createWithImagesLink && (
        <CreateComment
          repository={repository}
          pullRequest={pullRequest}
          url={createLink}
          commentWithImageUrl={createWithImagesLink}
        />
      )}
    </div>
  );
};

export default RootCommentContainer;
