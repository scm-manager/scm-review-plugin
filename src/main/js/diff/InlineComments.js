//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";

type Props = {
  children: React.Node,

  // context props
  classes: any
};

const styles = {
  comments: {
    backgroundColor: "whitesmoke", // $background
    borderTop: "1px solid #dbdbdb", // $border
    borderBottom: "1px solid #dbdbdb", // $border

    "& > *": {
      backgroundColor: "white" // $white
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
    return (
      <div className={classNames("is-indented-line", classes.comments)}>
        {children}
      </div>
    );
  }
}

export default injectSheet(styles)(InlineComments);
