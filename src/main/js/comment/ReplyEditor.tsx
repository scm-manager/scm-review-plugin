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
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CreateComment from "./CreateComment";
import { Comment, PullRequest } from "../types/PullRequest";
import { Link, Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  comment: Comment;
  onCancel: () => void;
};

const ReplyEditor: FC<Props> = ({ repository, pullRequest, comment, onCancel }) => {
  if (!comment._links.reply || !comment._links.replyWithImages) {
    throw new Error("reply links is missing");
  }

  const replyLink = comment._links.reply as Link;
  const replyWithImageLink = comment._links.replyWithImages as Link;
  return (
    <CommentSpacingWrapper>
      <CreateComment
        repository={repository}
        pullRequest={pullRequest}
        url={replyLink.href}
        commentWithImageUrl={replyWithImageLink.href}
        onCancel={onCancel}
        autofocus={true}
        reply={true}
      />
    </CommentSpacingWrapper>
  );
};

export default ReplyEditor;
