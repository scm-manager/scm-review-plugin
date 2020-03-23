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

const WrapperRoot = styled.div.attrs(props => ({
  className: "inline-comment"
}))`
  font-size: 0.9rem;
  padding: 1.5rem;

  & .content:not(:last-child) {
    margin-bottom: 0;
  }
`;

const WrapperChild = styled(WrapperRoot)`
  margin-left: 2rem;
`;

const Arrow = styled.div`
  position: absolute;
  margin-left: -1.5em;
`;

type Props = {
  isChildComment?: boolean;
  children: ReactNode;
};

class CommentSpacingWrapper extends Component<Props> {
  renderRoot = (children: ReactNode) => {
    return <WrapperRoot>{children}</WrapperRoot>;
  };

  renderChild = (children: ReactNode) => {
    return (
      <WrapperChild>
        <Arrow className="has-text-grey-lighter">
          <i className="fas fa-caret-right" />
        </Arrow>
        {children}
      </WrapperChild>
    );
  };

  render() {
    const { children, isChildComment } = this.props;
    if (isChildComment) {
      return this.renderChild(children);
    }
    return this.renderRoot(children);
  }
}

export default CommentSpacingWrapper;
