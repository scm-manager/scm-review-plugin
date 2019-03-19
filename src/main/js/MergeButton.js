// @flow
import React from "react";
import { translate } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import type { PullRequest } from "./types/PullRequest";
import type { Repository } from "@scm-manager/ui-types";

type Props = {
  merge: () => void,
  repository: Repository,
  mergeHasNoConflict: boolean,
  loading: boolean,
  pullRequest: PullRequest,
  t: string => string
};

type State = {
  mergeInformation: boolean
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeInformation: false
    };
  }

  showInformation = () => {
    this.setState({
      mergeInformation: true
    });
  };

  closeInformation = () => {
    this.setState({
      mergeInformation: false
    });
  };

  confirmMerge = () => {
    const { t, merge } = this.props;
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
          onClick: () => merge()
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
    const {
      t,
      loading,
      mergeHasNoConflict,
      repository,
      pullRequest
    } = this.props;
    const { mergeInformation } = this.state;
    const action = mergeHasNoConflict ? this.confirmMerge : this.showInformation;
    const color = mergeHasNoConflict ? "primary" : "warning";
    return (
      <p className="control">
        <Button
          label={t(
            "scm-review-plugin.show-pull-request.mergeButton.button-title"
          )}
          loading={loading}
          action={action}
          color={color}
        />
        <ManualMergeInformation
          showMergeInformation={mergeInformation}
          repository={repository}
          pullRequest={pullRequest}
          onClose={() => this.closeInformation()}
        />
      </p>
    );
  }
}

export default translate("plugins")(MergeButton);
