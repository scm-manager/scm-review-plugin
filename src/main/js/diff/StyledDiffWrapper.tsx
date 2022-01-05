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
import React, { Component, ReactNode } from "react";
import styled from "styled-components";

type Props = {
  children: ReactNode;
  commentable: boolean;
};

//TODO: Ersetzen!
const CommentableWrapper = styled.div`
  & table.diff tr:hover > td {
    background-color: #fff7d5 !important; // warning-25
  }

  tbody.commentable .diff-gutter:hover::after {
    font-family: "Font Awesome 5 Free";
    content: " \\f075";
    color: #33b2e8;
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
