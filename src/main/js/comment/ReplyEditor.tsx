import React, { FC } from "react";
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CreateComment from "./CreateComment";
import { Comment } from "../types/PullRequest";
import { Link } from "@scm-manager/ui-types";

type Props = {
  comment: Comment;
  onCreation: (comment: Comment) => void;
  onCancel: () => void;
};

const ReplyEditor: FC<Props> = ({ comment, onCreation, onCancel }) => {
  if (!comment._links.reply) {
    throw new Error("reply links is missing");
  }

  const replyLink = comment._links.reply as Link;
  return (
    <CommentSpacingWrapper>
      <CreateComment url={replyLink.href} onCreation={onCreation} onCancel={onCancel} autofocus={true} reply={true} />
    </CommentSpacingWrapper>
  );
};

export default ReplyEditor;
