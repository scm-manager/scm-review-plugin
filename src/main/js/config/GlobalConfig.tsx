import React from "react";
import { Configuration } from "@scm-manager/ui-components";
import ConfigEditor from "./ConfigEditor";

type Props = {
  link: string;
};

class GlobalConfig extends React.Component<Props> {
  render() {
    const { link } = this.props;
    return <Configuration link={link} render={props => <ConfigEditor {...props} global={true} />} />;
  }
}

export default GlobalConfig;
