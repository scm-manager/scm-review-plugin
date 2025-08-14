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
import { findLatestTransition } from "./transitions";
import { Comment } from "../types/PullRequest";
import styled from "styled-components";

type Props = {
  comment: Comment;
};

const Wrapper = styled.small`
  color: #9a9a9a; // dark-50
`;

const Editor = styled.span`
  color: #9a9a9a; // dark-50
`;

const LastEdited: FC<Props> = ({ comment }) => {
  const { t } = useTranslation("plugins");
  if (comment._embedded && comment._embedded.transitions) {
    const latestTransition = findLatestTransition(comment, "CHANGE_TEXT");
    if (latestTransition && latestTransition.user.id !== comment.author.id) {
      const latestEditor = latestTransition.user.displayName;
      return (
        <Wrapper>
          {t("scm-review-plugin.comment.lastEdited")} <Editor>{latestEditor}</Editor>
        </Wrapper>
      );
    }
  }
  return null;
};

export default LastEdited;
