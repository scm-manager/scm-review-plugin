// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = {
  reject: () => void,
  loading: boolean,
  t: string => string
};

class RejectButton extends React.Component<Props> {
  confirmReject = () => {
    const { t, reject } = this.props;
    confirmAlert({
      title: t(
        "scm-review-plugin.show-pullRequest.rejectButton.confirm-alert.title"
      ),
      message: t(
        "scm-review-plugin.show-pullRequest.rejectButton.confirm-alert.message"
      ),
      buttons: [
        {
          label: t(
            "scm-review-plugin.show-pullRequest.rejectButton.confirm-alert.submit"
          ),
          onClick: () => reject()
        },
        {
          label: t(
            "scm-review-plugin.show-pullRequest.rejectButton.confirm-alert.cancel"
          ),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const { loading, t } = this.props;
    const color = "warning";
    const action = this.confirmReject;
    return (
      <p className="control">
        <Button
          label={t(
            "scm-review-plugin.show-pullRequest.rejectButton.button-title"
          )}
          action={action}
          loading={loading}
          color={color}
        />
      </p>
    );
  }
}

export default translate("plugins")(RejectButton);
