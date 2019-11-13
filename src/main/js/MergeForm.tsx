import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Link } from "@scm-manager/ui-types";
import MergeStrategies from "./MergeStrategies";
import { Checkbox, Textarea } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  strategyLinks: Link[];
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  commitMessage: string;
  onChangeCommitMessage: (message: string) => void;
  shouldDeleteSourceBranch: boolean;
  onChangeDeleteSourceBranch: (value: boolean) => void;
  disabled: boolean;
};

class MergeForm extends React.Component<Props> {
  isCommitMessageDisabled = () => {
    return this.props.disabled || this.props.selectedStrategy === "FAST_FORWARD_IF_POSSIBLE";
  };

  render() {
    const {
      strategyLinks,
      selectedStrategy,
      selectStrategy,
      commitMessage,
      onChangeCommitMessage,
      shouldDeleteSourceBranch,
      onChangeDeleteSourceBranch,
      t
    } = this.props;

    return (
      <>
        <MergeStrategies
          strategyLinks={strategyLinks}
          selectedStrategy={selectedStrategy}
          selectStrategy={selectStrategy}
        />
        <hr />
        <Textarea
          placeholder={t("scm-review-plugin.showPullRequest.commitMessage")}
          disabled={this.isCommitMessageDisabled()}
          value={commitMessage}
          onChange={onChangeCommitMessage}
        />
        <hr />
        <Checkbox
          label={t("scm-review-plugin.showPullRequest.deleteSourceBranch.flag")}
          checked={shouldDeleteSourceBranch}
          helpText={t("scm-review-plugin.showPullRequest.deleteSourceBranch.help")}
          onChange={onChangeDeleteSourceBranch}
        />
      </>
    );
  }
}

export default withTranslation("plugins")(MergeForm);
