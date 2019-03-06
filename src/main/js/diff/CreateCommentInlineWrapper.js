//@flow
import * as React from "react";
import injectSheet from "react-jss";

const styles = {
  wrapper: {
    fontSize: "1rem",
    padding: "1rem",

    "& + &": {
      borderTop: "1px solid #dbdbdb"
    }
  }
};

type Props = {
  children: React.Node,
  // context props
  classes: any
}

class CreateCommentInlineWrapper extends React.Component<Props> {

  render() {
    const { classes, children } = this.props;
    return (
      <div className={classes.wrapper}>
        { children }
      </div>
    );
  }

}

export default injectSheet(styles)(CreateCommentInlineWrapper);
