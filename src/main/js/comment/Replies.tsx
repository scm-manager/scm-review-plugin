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
