//@flow
import * as React from "react";
import "./StyledDiffWrapper.css";

type Props = {
  children: React.Node,
};

class StyledDiffWrapper extends React.Component<Props> {

  render() {
    const { children } = this.props;
    return (
      <div className="commentable">
        { children }
      </div>
    );
  }

}

export default StyledDiffWrapper;
