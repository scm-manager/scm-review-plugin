//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  wrapperRoot: {
    fontSize: "1rem",
    padding: "1rem",
    borderTop: "1px solid #dbdbdb",
  },
  wrapperChild: {
    marginLeft: "2rem",
  },
  arrow: {
    position: "absolute",
    margin: "1rem 0 0 2em",
    fontSize: "16px",
    color: "#e5e5e5"
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
      <>
        {isChildComment && <div className={classes.arrow}><i className="fas fa-caret-right" /></div>}
        <div className={classNames(classes.wrapperRoot, isChildComment ? classes.wrapperChild : "")}>
          {children}
        </div>
      </>
    );
  }
}

export default injectSheet(styles)(CreateCommentInlineWrapper);
