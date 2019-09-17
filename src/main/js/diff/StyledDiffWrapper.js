//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";
import "./StyledDiffWrapper.css";

type Props = {
  children: React.Node,
  commentable: boolean,

  // context props
  classes: any
};

const styles = {
  commentable: {
    "& table.diff tr:hover td": {
      backgroundColor: "var(--warning-25) !important"
    }
  }
};

class StyledDiffWrapper extends React.Component<Props> {
  render() {
    const { children, commentable, classes } = this.props;
    return (
      <div
        className={
          commentable ? classNames("commentable", classes.commentable) : ""
        }
      >
        {children}
      </div>
    );
  }
}

export default injectSheet(styles)(StyledDiffWrapper);
