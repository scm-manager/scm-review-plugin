import React, { Component, ReactNode } from "react";
import styled from "styled-components";
import "./StyledDiffWrapper.css";

type Props = {
  children: ReactNode;
  commentable: boolean;
};

const CommentableWrapper = styled.div`
  & table.diff tr:hover > td {
    background-color: #fff7d5 !important; // warning-25
  }
`;

class StyledDiffWrapper extends Component<Props> {
  render() {
    const { children, commentable } = this.props;
    if (commentable) {
      return <CommentableWrapper className="commentable">{children}</CommentableWrapper>;
    }
    return <div>{children}</div>;
  }
}

export default StyledDiffWrapper;
