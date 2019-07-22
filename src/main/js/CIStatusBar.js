//@flow
import React from "react";
import ExtensionPoint from "@scm-manager/ui-extensions/lib/ExtensionPoint";
import classNames from "classnames";
import injectSheet from "react-jss";
import { translate } from "react-i18next";

type Props = {
  t: string => string,
  ciStatus: any,
  onClose: () => void,
  classes: any
};

type State = {
  openModal: boolean
}

const styles = {
  bar: {
    lineHeight: "2.5rem",
    margin: "10px 0px",
    paddingLeft: "10px"
  },
  message: {
    margin: "0px 5px",
  }
};

class CIStatusBar extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);

    this.state = {
      modalOpen: true,
      icon: null,
      color: null
    };
  }

  componentDidMount() {
    const {ciStatus} = this.props;

    this.setState({ icon: "exclamation-circle", color: "warning" });
    return;

    if(ciStatus.filter(ci => ci.status === "UNSTABLE").length > 0) {
      this.setState({ icon: "exclamation-circle", color: "warning" });
      return;
    }
    if(ciStatus.filter(ci => ci.status === "FAILURE").length > 0) {
      this.setState({ icon: "times-circle", color: "danger" });
      return;
    }
    if(ciStatus.every(ci => ci.status === "SUCCESS")) {
      this.setState({ icon: "check-circle", color: "success" });
      return;
    }
    if(ciStatus.every(ci => ci.status === "ABORTED" || ci.status === "PENDING"))
      this.setState({icon: "circle-notch", color: "lightgrey" })

  }

  render() {
    const { ciStatus, classes, t } = this.props;
    const { icon, color } = this.state;

    // const errors = ciStatus.filter(ci => ci.status === "FAILURE" || ci.status === "UNSTABLE").length;

    return (
      <>
        {
          this.state.modalOpen &&
          <ExtensionPoint
            name="ciPlugin.modal"
            renderAll={true}
            props={this.props}
          />
        }

        <div className={classNames(classes.bar, `is-full-width has-background-${color} has-text-white`)} onClick={() => this.setState({ modalOpen: true })}>
          <i className={`fas fa-1x fa-${icon}`}/>
          <span className={classNames(classes.message, "has-text-weight-bold")}> {/*ciStatus.length*/} 2 {t("scm-review-plugin.pull-request.ci-status-bar.analysis-message")}</span>
          <i className={"fas fa-chevron-right"}/>
          <span className={classNames(classes.message)}> {/*errors*/} 1 {t("scm-review-plugin.pull-request.ci-status-bar.result-message")}</span>
        </div>
      </>
    )

  }
}

export default injectSheet(styles)(translate("plugins")(CIStatusBar));
