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

import React, { FC } from "react";
import { Comment, PullRequest } from "../types/PullRequest";
import PullRequestComment from "./PullRequestComment";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  comment: Comment;
  createLink?: string;
  createWithImagesLink?: string;
};

const Replies: FC<Props> = ({ repository, pullRequest, comment, createLink, createWithImagesLink }) => {
  if (!comment._embedded || !comment._embedded.replies) {
    return null;
  }

  const replies = comment._embedded.replies;
  return (
    <>
      {replies.map((reply: Comment) => (
        <PullRequestComment
          repository={repository}
          pullRequest={pullRequest}
          key={reply.id}
          parent={comment}
          createLink={createLink}
          createWithImageLink={createWithImagesLink}
          comment={reply}
        />
      ))}
    </>
  );
};

export default Replies;
