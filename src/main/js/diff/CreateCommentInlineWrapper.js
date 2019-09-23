//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  wrapperRoot: {
    fontSize: "0.9rem",
    padding: "1.5rem",
    borderTop: "1px solid #dbdbdb", // border

    "& .content:not(:last-child)": {
      marginBottom: "0"
    }
  },
  wrapperChild: {
    marginLeft: "2rem"
  },
  arrow: {
    position: "absolute",
    margin: "1.5rem 0 0 2.5em",
    fontSize: "16px"
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
        {isChildComment && (
          <div className={classNames("has-text-grey-lighter", classes.arrow)}>
            <i className="fas fa-caret-right" />
          </div>
        )}
        <div
          className={classNames(
            classes.wrapperRoot,
            isChildComment ? classes.wrapperChild : ""
          )}
        >
          {children}
        </div>
      </>
    );
  }
}

export default injectSheet(styles)(CreateCommentInlineWrapper);
