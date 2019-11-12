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
