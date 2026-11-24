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
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router";
import styled from "styled-components";
import { AnnotationFactory, DiffFile, FileControlFactory, Modal } from "@scm-manager/ui-components";
import { Icon, LinkButton } from "@scm-manager/ui-core";
import { createChangeIdFromLocation } from "../diff/locations";
import { Comment } from "../types/PullRequest";
import { mapCommentToFile } from "./commentToFileMapper";
import ContextInlineComment from "./ContextInlineComment";

const ContextFileControl: FileControlFactory = file => {
  const location = useLocation();
  const { t } = useTranslation("plugins");

  if (location.pathname.includes("comments")) {
    const path = file.newPath ?? file.oldPath;
    return (
      <LinkButton
        to={location.pathname.replace("comments/", `diff/#diff-${encodeURIComponent(path)}`)}
        title={t("scm-review-plugin.diff.jumpToSource")}
      >
        <Icon>file-code</Icon>
      </LinkButton>
    );
  }
};

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
            fileControlFactory={ContextFileControl}
            sideBySide={false}
          />
        </>
      }
      active={true}
    />
  );
};

export default ContextModal;
