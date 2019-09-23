//@flow
import * as React from "react";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  wrapperRoot: {
    fontSize: "0.9rem",
    padding: "1.5rem",

    "& .content:not(:last-child)": {
      marginBottom: "0"
    }
  },
  wrapperChild: {
    marginLeft: "2rem"
  },
  arrow: {
    position: "absolute",
    marginLeft: "-1.5em"
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
      <div
        className={classNames(
          "inline-comment",
          classes.wrapperRoot,
          isChildComment ? classes.wrapperChild : ""
        )}
      >
        {isChildComment && (
          <div className={classNames("has-text-grey-lighter", classes.arrow)}>
            <i className="fas fa-caret-right" />
          </div>
        )}
        {children}
      </div>
    );
  }
}

export default injectSheet(styles)(CreateCommentInlineWrapper);
