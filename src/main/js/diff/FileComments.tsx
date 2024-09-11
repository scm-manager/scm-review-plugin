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

const Wrapper = styled.div`
  border-bottom: 1px solid #dbdbdb; // border

  /* reply on same file as previous comment */
  & .comment-wrapper + .inline-comment {
    border-top: 1px solid #dbdbdb; // $border
  }

  /* multiple comments on same file */
  & .comment-wrapper + .comment-wrapper {
    border-top: 1px solid #dbdbdb; // $border
  }
`;

type Props = {
  children: ReactNode;
};

class FileComments extends Component<Props> {
  render() {
    const { children } = this.props;
    return <Wrapper>{children}</Wrapper>;
  }
}

export default FileComments;
