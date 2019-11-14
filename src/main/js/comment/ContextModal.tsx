import React, { FC } from "react";
import { Modal, DiffFile, AnnotationFactory } from "@scm-manager/ui-components";
import { mapCommentToFile } from "./commentToFileMapper";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { Comment } from "../types/PullRequest";
import { createChangeIdFromLocation } from "../diff/locations";

import ContextInlineComment from "./ContextInlineComment";

const StyledModal = styled(Modal)`
  & table.diff .diff-gutter:empty:hover::after {
    display: none;
  }

  & .modal-card {
    @media screen and (min-width: 1280px), print {
      width: 1200px;
    }
  }
`;

type Props = {
  comment: Comment;
  onClose: () => void;
};

const createInlineCommentAnnotationFactory = (comment: Comment): AnnotationFactory => {
  return () => {
    const annotations: { [key: string]: React.ReactNode } = {};
    if (comment.location) {
      const changeId = createChangeIdFromLocation(comment.location);
      annotations[changeId] = <ContextInlineComment comment={comment} />;
    }
    return annotations;
  };
};

const ContextModal: FC<Props> = ({ comment, onClose }) => {
  const { t } = useTranslation("plugins");

  return (
    <StyledModal
      title={t("scm-review-plugin.comment.contextModal.title")}
      closeFunction={onClose}
      body={
        <>
          <strong>{t("scm-review-plugin.comment.contextModal.message")}</strong>
          <br />
          <br />
          <DiffFile
            file={mapCommentToFile(comment)}
            defaultCollapse={false}
            annotationFactory={createInlineCommentAnnotationFactory(comment)}
            sideBySide={false}
          />
        </>
      }
      active={true}
    />
  );
};

export default ContextModal;
