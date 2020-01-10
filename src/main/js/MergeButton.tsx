import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, Tooltip } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import { MergeCheck, MergeCommit, PullRequest } from "./types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import MergeModal from "./MergeModal";

type Props = WithTranslation & {
  merge: (strategy: string, commit: MergeCommit) => void;
  repository: Repository;
  mergeCheck?: MergeCheck;
  loading: boolean;
  pullRequest: PullRequest;
};

type State = {
  mergeInformation: boolean;
  showMergeModal: boolean;
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeInformation: false,
      showMergeModal: false
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

  toggleMergeModal = () => {
    this.setState(prevState => ({
      showMergeModal: !prevState.showMergeModal
    }));
  };

  renderButton = () => {
    const { t, loading, mergeCheck } = this.props;

    const action = mergeCheck?.hasConflicts ? this.showInformation : this.toggleMergeModal;
    const color = mergeCheck?.hasConflicts ? "warning" : "primary";
    const checkHints = mergeCheck ? mergeCheck.mergeObstacles.map(o => t(o.key)).join("\n") : "";
    const disabled = mergeCheck && mergeCheck.mergeObstacles.length > 0;

    const button = (
      <Button
        label={t("scm-review-plugin.showPullRequest.mergeButton.buttonTitle")}
        loading={loading}
        action={action}
        color={color}
        disabled={disabled}
        icon={checkHints ? "exclamation-triangle" : ""}
      />
    );
    if (checkHints) {
      return (
        <Tooltip message={checkHints} location={"top"}>
          {button}
        </Tooltip>
      );
    } else {
      return button;
    }
  };

  render() {
    const { repository, pullRequest, merge } = this.props;
    const { mergeInformation, showMergeModal } = this.state;

    if (showMergeModal) {
      return (
        <MergeModal
          merge={(strategy: string, mergeCommit: MergeCommit) => merge(strategy, mergeCommit)}
          close={this.toggleMergeModal}
          pullRequest={pullRequest}
        />
      );
    }

    return (
      <p className="control">
        {this.renderButton()}
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

export default withTranslation("plugins")(MergeButton);
