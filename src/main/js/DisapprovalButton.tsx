import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  loading: boolean;
  action: () => void;
};

type State = {};

class DisapprovalButton extends React.Component<Props, State> {
  render() {
    const { loading, action, t } = this.props;
    return (
      <Button
        label={t("scm-review-plugin.pullRequest.details.buttons.disapprove")}
        loading={loading}
        action={action}
        color="link is-outlined"
        icon="times"
      />
    );
  }
}

export default withTranslation("plugins")(DisapprovalButton);
