// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";
import injectSheet from "react-jss";

type Props = {
  reject: () => void,
  loading: boolean,
  t: string => string
};

const styles = {
  buttonSpace: {
    marginBottom: "1em",
    marginRight: "0.75em"
  }
};

class RejectButton extends React.Component<Props> {
  confirmReject = () => {
    const { t, reject } = this.props;
    confirmAlert({
      title: t(
        "scm-review-plugin.show-pull-request.rejectButton.confirm-alert.title"
      ),
      message: t(
        "scm-review-plugin.show-pull-request.rejectButton.confirm-alert.message"
      ),
      buttons: [
        {
          label: t(
            "scm-review-plugin.show-pull-request.rejectButton.confirm-alert.submit"
          ),
          onClick: () => reject()
        },
        {
          label: t(
            "scm-review-plugin.show-pull-request.rejectButton.confirm-alert.cancel"
          ),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const { loading, t, classes } = this.props;
    const color = "warning";
    const action = this.confirmReject;
    return (
      <Button
        label={t(
          "scm-review-plugin.show-pull-request.rejectButton.button-title"
        )}
        action={action}
        loading={loading}
        color={color}
        className={classes.buttonSpace}
      />
    );
  }
}

export default injectSheet(styles)(translate("plugins")(RejectButton));
