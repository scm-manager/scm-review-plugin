// @flow
import React from "react";
import {translate} from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";
import type { Permission } from "@scm-manager/ui-types";

type Props = {
  permission: Permission,
  namespace: string,
  repoName: string,
  confirmDialog?: boolean,
  t: string => string,
  mergeLoading,
  mergePermission: (
    permission: Permission,
    namespace: string,
    repoName: string
  ) => void
};

type State = {

};

class PullRequestButton extends React.Component<Props, State> {
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

  mergePermission = () => {
    this.props.mergePermission(
      this.props.permission,
      this.props.namespace,
      this.props.repoName
    );
  };

  confirmMerge = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.title"),
      message: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.show-pull-request.mergeButton.confirm-alert.submit"),
          onClick: () => this.mergePermission()
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
    const action = confirmDialog ? this.confirmMerge : this.mergePermission;
    return (
      <Button label={t("scm-review-plugin.show-pull-request.mergeButton.button-title")} loading={this.props.mergeLoading} action={action} color="primary" />
      /* scm-review-plugin.show-pull-request.mergeButton" */
    );
  }
}

export default translate("plugins")(PullRequestButton);
