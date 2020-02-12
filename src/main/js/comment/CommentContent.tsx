import React, { FC, useEffect, useState } from "react";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import { MarkdownView } from "@scm-manager/ui-components";
import styled from "styled-components";

const MarkdownWrapper = styled.div`
  .content {
    > h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      margin: 0.5rem 0 !important;
      font-size: 0.9rem !important;
    }
    > h1 {
      font-weight: 700;
    }
    > h2 {
      font-weight: 600;
    }
    > h3,
    h4,
    h5,
    h6 {
      font-weight: 500;
    }
    & strong {
      font-weight: 500;
    }
  }
`;

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

  return (
    <MarkdownWrapper>
      <MarkdownView content={message} />
    </MarkdownWrapper>
  );
};

export default CommentContent;
