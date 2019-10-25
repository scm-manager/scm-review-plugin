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
  isChildComment: boolean;
  children: ReactNode;
};

class CreateCommentInlineWrapper extends Component<Props> {
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

export default CreateCommentInlineWrapper;
