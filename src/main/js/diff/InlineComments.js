//@flow
import * as React from "react";
import injectSheet from "react-jss";

type Props = {
  children: React.Node,

  // context props
  classes: any
};

const styles = {
  comments: {
    paddingLeft: "6.5rem",
    backgroundColor: "whitesmoke", // background
    borderTop: "1px solid #dbdbdb", // border
    borderBottom: "1px solid #dbdbdb", // border

    "& > *": {
      backgroundColor: "white" // white
    },

    /* reply on same line as inline comment */
    "& .comment-wrapper + .inline-comment": {
      borderTop: "1px solid #dbdbdb" // $border
    }
  }
};

class InlineComments extends React.Component<Props> {
  render() {
    const { children, classes } = this.props;
    return <div className={classes.comments}>{children}</div>;
  }
}

export default injectSheet(styles)(InlineComments);
