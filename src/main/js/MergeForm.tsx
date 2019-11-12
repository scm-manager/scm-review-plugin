import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Link } from "@scm-manager/ui-types";
import MergeStrategies from "./MergeStrategies";
import { Textarea } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  strategyLinks: Link[];
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  commitMessage: string;
  onChangeCommitMessage: (message: string) => void;
};

class MergeForm extends React.Component<Props> {
  isCommitMessageDisabled = () => {
    return this.props.selectedStrategy === "FAST_FORWARD_IF_POSSIBLE";
  };

  render() {
    const { strategyLinks, selectedStrategy, selectStrategy, commitMessage, onChangeCommitMessage, t } = this.props;

    return (
      <>
        <MergeStrategies
          strategyLinks={strategyLinks}
          selectedStrategy={selectedStrategy}
          selectStrategy={selectStrategy}
        />
        <hr />
        <Textarea
          placeholder={t("scm-review-plugin.show-pull-request.commit-message")}
          disabled={this.isCommitMessageDisabled()}
          value={commitMessage}
          onChange={onChangeCommitMessage}
        />
      </>
    );
  }
}

export default withTranslation("plugins")(MergeForm);
