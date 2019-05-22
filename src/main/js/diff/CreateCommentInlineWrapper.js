//@flow
import * as React from "react";
import injectSheet from "react-jss";

const styles = {
  wrapperRoot: {
    fontSize: "1rem",
    padding: "1rem",
    borderTop: "1px solid #dbdbdb",
  },

  wrapperChild: {
    fontSize: "1rem",
    padding: "1rem",
    marginLeft: "30px",
    borderTop: "1px solid #dbdbdb",
  }
};

type Props = {
  children: React.Node,
  // context props
  classes: any,
  isChildComment: boolean
};

class CreateCommentInlineWrapper extends React.Component<Props> {
  render() {
    const { classes, children, isChildComment } = this.props;
    return (
        <div className={isChildComment ? classes.wrapperChild : classes.wrapperRoot}>
            {children}
        </div>
    );
  }
}

export default injectSheet(styles)(CreateCommentInlineWrapper);
