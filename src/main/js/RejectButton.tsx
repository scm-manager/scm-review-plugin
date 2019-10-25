import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  reject: () => void;
  loading: boolean;
};

class RejectButton extends React.Component<Props> {
  confirmReject = () => {
    const { t, reject } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.show-pull-request.rejectButton.confirm-alert.title"),
      message: t("scm-review-plugin.show-pull-request.rejectButton.confirm-alert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.show-pull-request.rejectButton.confirm-alert.submit"),
          onClick: () => reject()
        },
        {
          label: t("scm-review-plugin.show-pull-request.rejectButton.confirm-alert.cancel"),
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
          label={t("scm-review-plugin.show-pull-request.rejectButton.button-title")}
          action={action}
          loading={loading}
          color={color}
        />
      </p>
    );
  }
}

export default withTranslation("plugins")(RejectButton);