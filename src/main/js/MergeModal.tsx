import React from "react";
import { Modal } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import classNames from "classnames";
import MergeForm from "./MergeForm";
import {MergeCommit, PullRequest} from "./types/PullRequest";
import { Link } from "@scm-manager/ui-types";

type Props = WithTranslation & {
  close: () => void;
  merge: (strategy: string, mergeCommit: MergeCommit) => void;
  pullRequest: PullRequest;
};

type State = {
  mergeStrategy: string;
  mergeCommit: MergeCommit;
};

class MergeModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeStrategy: "MERGE_COMMIT",
      mergeCommit: {
        commitMessage: "",
        source: this.props.pullRequest.source,
        target: this.props.pullRequest.target,
        author: this.props.pullRequest.author
      }
    };
  }

  selectStrategy = (strategy: string) => {
    this.setState({
      mergeStrategy: strategy
    });
  };

  onChangeCommitMessage = (newMessage: string) => {
    this.setState({ mergeCommit: { ...this.state.mergeCommit, commitMessage: newMessage} });
  };

  render() {
    const { pullRequest, merge, close, t } = this.props;
    const { mergeStrategy, mergeCommit } = this.state;

    const buttons = [
      {
        label: t("scm-review-plugin.show-pull-request.merge-modal.cancel"),
        onClick: () => close()
      },
      {
        label: t("scm-review-plugin.show-pull-request.merge-modal.merge"),
        onClick: () => merge(mergeStrategy, mergeCommit)
      }
    ];

    const footer = (
      <div className="field is-grouped">
        {buttons.map((button, i) => (
          <p className="control">
            <a
              className={classNames("button", i === 1 ? "is-info" : "is-grey")}
              key={i}
              onClick={() => button.onClick()}
            >
              {button.label}
            </a>
          </p>
        ))}
      </div>
    );

    const body = (
      <MergeForm
        selectedStrategy={mergeStrategy}
        selectStrategy={this.selectStrategy}
        strategyLinks={pullRequest._links.merge as Link[]}
        commitMessage={mergeCommit.commitMessage}
        onChangeCommitMessage={this.onChangeCommitMessage}
      />
    );

    return (
      <Modal
        title={t("scm-review-plugin.show-pull-request.merge-modal.title")}
        active={true}
        body={body}
        closeFunction={close}
        footer={footer}
      />
    );
  }
}

export default withTranslation("plugins")(MergeModal);
