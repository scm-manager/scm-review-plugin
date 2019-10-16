//@flow
import * as React from "react";
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
  margin-Left: 2rem;
`;

const Arrow = styled.div`
  position: absolute;
  margin-left: -1.5em;
`;

type Props = {
  isChildComment: boolean,
  children: React.Node
};

class CreateCommentInlineWrapper extends React.Component<Props> {

  renderRoot = (children: React.Node) => {
    return <WrapperRoot>{children}</WrapperRoot>
  };

  renderChild = (children: React.Node) => {
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
