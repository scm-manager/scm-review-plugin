import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import MergeModal from "./MergeModal";

type Props = WithTranslation & {
  merge: (strategy: string, commit: MergeCommit) => void;
  repository: Repository;
  mergeHasNoConflict?: boolean;
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

  render() {
    const { t, loading, mergeHasNoConflict, repository, pullRequest, merge } = this.props;
    const { mergeInformation, showMergeModal } = this.state;
    const action = mergeHasNoConflict ? this.toggleMergeModal : this.showInformation;
    const color = mergeHasNoConflict ? "primary" : "warning";

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
        <Button
          label={t("scm-review-plugin.showPullRequest.mergeButton.buttonTitle")}
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

export default withTranslation("plugins")(MergeButton);
