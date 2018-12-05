// @flow
import React from "react";
import {translate} from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = {
  namespace: string,
  repoName: string,
  confirmDialog?: boolean,
  t: string => string
};

type State = {

};

class MergeButton extends React.Component<Props, State> {
  static defaultProps = {
    confirmDialog: true
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      pullRequest: null
    };
  }


  confirmMerge = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.title"),
      message: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.submit"),
          onClick: () => null
        },
        {
          label: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const { confirmDialog, t } = this.props;
    const action = confirmDialog ? this.confirmMerge : null;
    return (
      <Button label={t("scm-review-plugin.show-pull-request.mergeButton.button-title")} loading={this.props.loading} action={action} color="primary" />
      /* scm-review-plugin.show-pull-request.mergeButton" */
    );
  }
}

export default translate("plugins")(MergeButton);
