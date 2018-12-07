// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import type {PullRequest} from "./types/PullRequest";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  merge: (Repository) => void,
  t: string => string
};

type State = {
  loading: boolean,
  color: string,
  confirmDialog?: boolean
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      color: "primary",
      loading: false,
      confirmDialog: true
    };
  }

  clicked = () => {
    const {repository, merge, pullRequest} = this.props;
    merge(repository, pullRequest);
};

  confirmMerge = () => {
    const { t } = this.props;
    confirmAlert({
      title: t(
        "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.title"
      ),
      message: t(
        "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.message"
      ),
      buttons: [
        {
          label: t(
            "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.submit"
          ),
          onClick: () => this.clicked()
        },
        {
          label: t(
            "scm-review-plugin.show-pull-request.mergeButton.confirm-alert.cancel"
          ),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const { t, clicked } = this.props;
    const { confirmDialog, loading, color } = this.state;
    const action = confirmDialog ? this.confirmMerge : this.clicked;
    return (
      <Button
        label={t(
          "scm-review-plugin.show-pull-request.mergeButton.button-title"
        )}
        loading={loading}
        action={action}
        color={color}
      />
    );
  }
}

export default translate("plugins")(MergeButton);
