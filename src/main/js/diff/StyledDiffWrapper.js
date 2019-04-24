//@flow
import * as React from "react";
import "./StyledDiffWrapper.css";

type Props = {
  children: React.Node,
  commentable: boolean
};

class StyledDiffWrapper extends React.Component<Props> {
  render() {
    const { children, commentable } = this.props;
    const classname = commentable ? "commentable" : "";
    return <div className={classname}>{children}</div>;
  }
}

export default StyledDiffWrapper;
