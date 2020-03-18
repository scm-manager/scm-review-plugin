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
import { DateFromNow } from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CommentMetadata from "./CommentMetadata";
import LastEdited from "./LastEdited";
import CommentContent from "./CommentContent";
import InlineComments from "../diff/InlineComments";

type Props = {
  comment: Comment;
};

const ContextInlineComment: FC<Props> = ({ comment }) => (
  <InlineComments>
    <CommentSpacingWrapper isChildComment={false}>
      <article className="media">
        <div className="media-content is-clipped content">
          <p>
            <strong>{comment.author.displayName}</strong>{" "}
            <CommentMetadata>
              <DateFromNow date={comment.date} /> <LastEdited comment={comment} />
            </CommentMetadata>
            <br />
            <CommentContent comment={comment} />
          </p>
        </div>
      </article>
    </CommentSpacingWrapper>
  </InlineComments>
);

export default ContextInlineComment;
