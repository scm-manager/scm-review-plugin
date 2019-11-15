import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  loading: boolean;
  action: () => void;
};

type State = {};

class ApprovalButton extends React.Component<Props, State> {
  render() {
    const { loading, action, t } = this.props;
    return (
      <Button
        label={t("scm-review-plugin.pullRequest.details.buttons.approve")}
        loading={loading}
        action={action}
        color="link is-outlined"
        icon="check"
      />
    );
  }
}

export default withTranslation("plugins")(ApprovalButton);
