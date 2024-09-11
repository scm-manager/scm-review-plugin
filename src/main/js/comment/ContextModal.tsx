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
import { AnnotationFactory, DiffFile, FileControlFactory, Modal } from "@scm-manager/ui-components";
import { mapCommentToFile } from "./commentToFileMapper";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { Comment } from "../types/PullRequest";
import { createChangeIdFromLocation, escapeWhitespace } from "../diff/locations";

import ContextInlineComment from "./ContextInlineComment";
import { Icon, LinkButton } from "@scm-manager/ui-buttons";
import { useHistory } from "react-router-dom";

const ContextFileControl: FileControlFactory = file => {
  const history = useHistory();
  const { t } = useTranslation("plugins");

  if (history.location.pathname.includes("comments")) {
    const path = file.newPath ?? file.oldPath;
    const escapedPath = escapeWhitespace(path);
    return (
      <LinkButton
        to={history.location.pathname.replace("comments/", `diff/#diff-${encodeURIComponent(escapedPath)}`)}
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
