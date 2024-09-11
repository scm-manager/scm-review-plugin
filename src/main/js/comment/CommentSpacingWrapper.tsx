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
