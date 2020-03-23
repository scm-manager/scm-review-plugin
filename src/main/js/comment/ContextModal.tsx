/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
