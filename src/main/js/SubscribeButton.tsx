import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, Icon } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  loading: boolean;
  action: () => void;
};

type State = {};

class SubscribeButton extends React.Component<Props, State> {
  render() {
    const { loading, action, t } = this.props;
    return (
      <Button
        loading={loading}
        action={action}
        title={t("scm-review-plugin.pullRequest.details.buttons.subscribe")}
        color="link is-outlined"
      >
        <Icon name="plus" color="inherit" />
      </Button>
    );
  }
}

export default withTranslation("plugins")(SubscribeButton);
