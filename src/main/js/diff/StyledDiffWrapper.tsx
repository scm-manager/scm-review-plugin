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

import React, { Component, ReactNode } from "react";
import styled from "styled-components";

type Props = {
  children: ReactNode;
  commentable: boolean;
};

const CommentableWrapper = styled.div`
  & table.diff tr:hover > td {
    background-color: var(--sh-selected-color) !important;
  }

  tbody.commentable .diff-gutter:hover::after {
    font-family: "Font Awesome 5 Free";
    content: " \\f075";
    color: var(--scm-column-selection);
  }

  tbody.expanded .diff-gutter {
    cursor: default;
  }
`;

const NotCommentableWrapper = styled.div`
  tbody.expanded .diff-gutter {
    cursor: default;
  }
`;

class StyledDiffWrapper extends Component<Props> {
  render() {
    const { children, commentable } = this.props;
    if (commentable) {
      return <CommentableWrapper>{children}</CommentableWrapper>;
    }
    return <NotCommentableWrapper>{children}</NotCommentableWrapper>;
  }
}

export default StyledDiffWrapper;
