import React, {Component, ReactNode} from "react";
import styled from "styled-components";

const Wrapper = styled.div`
  border-bottom: 1px solid #dbdbdb; // border

  /* reply on same file as previous comment */
  & .comment-wrapper + .inline-comment {
    border-top: 1px solid #dbdbdb; // $border
  }

  /* multiple comments on same file */
  "& .comment-wrapper + .comment-wrapper": {
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
