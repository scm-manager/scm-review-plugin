// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = {
  reject: () => void,
  t: string => string
};

class RejectButton extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
  }

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
    const { t } = this.props;
    const color = "warning";
    const action = this.confirmReject;
    return (
      <Button
        label={t(
          "scm-review-plugin.show-pull-request.rejectButton.button-title"
        )}
        action={action}
        color={color}
      />
    );
  }
}

export default translate("plugins")(RejectButton);
