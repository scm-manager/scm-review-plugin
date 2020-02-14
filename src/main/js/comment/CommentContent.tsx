import React, { FC, useEffect, useState } from "react";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import ReducedMarkdownView from "../ReducedMarkdownView";

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
      const matcher = new RegExp("@\\[" + m.id + "\\]", "g");
      content = content.replace(matcher, `[${m.displayName}](mailto:${m.mail})`);
    });
    setMessage(content);
  }, [comment]);

  return <ReducedMarkdownView content={message} />;
};

export default CommentContent;
