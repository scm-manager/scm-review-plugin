//@flow
import * as React from "react";
import injectSheet from "react-jss";

const styles = {
  wrapper: {
    borderBottom: "1px solid #dbdbdb", // border

    /* reply on same file as previous comment */
    "& .comment-wrapper + .inline-comment": {
      borderTop: "1px solid #dbdbdb" // $border
    },
    /* multiple comments on same file */
    "& .comment-wrapper + .comment-wrapper": {
      borderTop: "1px solid #dbdbdb" // $border
    }
  }
};

type Props = {
  children: React.Node,

  // context props
  classes: any
};

class FileComments extends React.Component<Props> {
  render() {
    const { children, classes } = this.props;
    return <div className={classes.wrapper}>{children}</div>;
  }
}

export default injectSheet(styles)(FileComments);
