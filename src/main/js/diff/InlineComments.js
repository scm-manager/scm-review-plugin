//@flow
import * as React from "react";
import injectSheet from "react-jss";

const styles = {
  comments: {
    borderTop: "1px solid #dbdbdb",
    borderBottom: "1px solid #dbdbdb"
  }
};

type Props = {
  children: React.Node,
  // context props
  classes: any
}

class InlineComments extends React.Component<Props> {

  render() {
    const { children, classes } = this.props;
    return (
      <div className={classes.comments}>
        {children}
      </div>
    );
  }

}

export default injectSheet(styles)(InlineComments);
