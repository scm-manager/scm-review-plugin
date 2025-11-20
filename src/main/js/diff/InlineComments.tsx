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
};

const Comments = styled.div`
  border-top: 1px solid #dbdbdb; // $border
  border-bottom: 1px solid #dbdbdb; // $border

  & > * {
    background-color: var(--scm-secondary-background);
  }

  /* reply on same line as inline comment */
  & .comment-wrapper + .inline-comment {
    border-top: 1px solid #dbdbdb; // $border
  }

  & .media-content {
    max-width: 100%;
  }
`;

class InlineComments extends Component<Props> {
  render() {
    const { children } = this.props;
    return <Comments className="is-indented-line has-background-secondary-less">{children}</Comments>;
  }
}

export default InlineComments;
