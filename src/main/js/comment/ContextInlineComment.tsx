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
