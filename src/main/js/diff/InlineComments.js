//@flow
import * as React from "react";
import styled from "styled-components";

type Props = {
  children: React.Node
};

const Comments = styled.div`
  background-color: whitesmoke; // $background
  border-top: 1px solid #dbdbdb; // $border
  border-bottom: 1px solid #dbdbdb; // $border

  & > * {
    background-color: white;
  }

  /* reply on same line as inline comment */
  & .comment-wrapper + .inline-comment {
    border-top: 1px solid #dbdbdb; // $border
  }
`;

class InlineComments extends React.Component<Props> {
  render() {
    const { children } = this.props;
    return (
      <Comments className="is-indented-line">
        {children}
      </Comments>
    );
  }
}

export default InlineComments;
