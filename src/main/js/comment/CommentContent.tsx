import React, { FC } from "react";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import { MarkdownView } from "@scm-manager/ui-components";

type Props = {
  comment: Comment;
};

const CommentContent: FC<Props> = ({ comment }) => {
  const { t } = useTranslation("plugins");
  const message = comment.systemComment
    ? t("scm-review-plugin.comment.systemMessage." + comment.comment)
    : comment.comment;
  return <MarkdownView content={message} />;
};

export default CommentContent;
