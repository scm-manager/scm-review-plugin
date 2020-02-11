import React, { FC, useEffect, useState } from "react";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import { MarkdownView } from "@scm-manager/ui-components";

type Props = {
  comment: Comment;
};

const CommentContent: FC<Props> = ({ comment }) => {
  const { t } = useTranslation("plugins");
  const [message, setMessage] = useState(
    comment.systemComment ? t("scm-review-plugin.comment.systemMessage." + comment.comment) : comment.comment
  );
  useEffect(() => {
    let content = message;
    comment.mentions.forEach(m => {
      let userId = m.id;
      let re = new RegExp("@\\[" + userId + "\\]", 'g');
      content = content.replace(re, `[${m.displayName}](mailto:${m.mail})`);
    });
    setMessage(content);
  }, [comment]);

  return <MarkdownView content={message} />;
};

export default CommentContent;
