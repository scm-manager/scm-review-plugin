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
