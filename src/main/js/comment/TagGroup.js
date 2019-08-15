// @flow
import React, {type Node} from "react";
import injectSheet from "react-jss";

const styles = {
  group: {
    "& > *": {
      marginRight: "0.5rem"
    }
  }
};

type Props = {
  children?: Node,
  // context props
  classes: any
};

class TagGroup extends React.Component<Props> {
  render() {
    const { children, classes } = this.props;
    return (
      <span className={classes.group}>
        { children }
      </span>
    );
  }
}

export default injectSheet(styles)(TagGroup);
