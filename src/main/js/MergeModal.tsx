import React from "react";
import { Button, Modal, SubmitButton } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import MergeForm from "./MergeForm";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { Link } from "@scm-manager/ui-types";
import { getDefaultCommitDefaultMessage } from "./pullRequest";

type Props = WithTranslation & {
  close: () => void;
  merge: (strategy: string, mergeCommit: MergeCommit) => void;
  pullRequest: PullRequest;
};

type State = {
  mergeStrategy: string;
  mergeCommit: MergeCommit;
  defaultCommitMessages: { [key: string]: string };
  loading: boolean;
  loadingDefaultMessage: boolean;
  messageChanged: boolean;
};

class MergeModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeStrategy: "MERGE_COMMIT",
      mergeCommit: {
        commitMessage: "",
        author: this.props.pullRequest.author,
        shouldDeleteSourceBranch: false
      },
      defaultCommitMessages: {},
      loading: false,
      loadingDefaultMessage: false,
      messageChanged: false
    };
  }
  componentDidMount(): void {
    this.getDefaultMessage();
  }

  getDefaultMessage = () => {
    const { pullRequest } = this.props;
    const { defaultCommitMessages, mergeCommit, mergeStrategy } = this.state;
    if (!!defaultCommitMessages[mergeStrategy]) {
      this.setState({ mergeCommit: { ...mergeCommit, commitMessage: defaultCommitMessages[mergeStrategy] } });
    } else if (pullRequest && pullRequest._links && pullRequest._links.defaultCommitMessage) {
      this.setState({ loadingDefaultMessage: true }, () => {
        getDefaultCommitDefaultMessage(
          (pullRequest._links.defaultCommitMessage as Link).href + "?strategy=" + mergeStrategy
        ).then(commitMessage => {
          defaultCommitMessages[mergeStrategy] = commitMessage;
          this.setState({
            defaultCommitMessages: defaultCommitMessages,
            mergeCommit: { ...mergeCommit, commitMessage: commitMessage },
            messageChanged: false,
            loadingDefaultMessage: false
          });
        });
      });
    }
  };

  selectStrategy = (strategy: string) => {
    this.setState(
      {
        mergeStrategy: strategy
      },
      () => {
        if (!this.state.messageChanged) {
          this.getDefaultMessage();
        }
      }
    );
  };

  onChangeCommitMessage = (newMessage: string) => {
    this.setState({ messageChanged: true, mergeCommit: { ...this.state.mergeCommit, commitMessage: newMessage } });
  };

  performMerge = () => {
    const { merge } = this.props;
    const { mergeStrategy, mergeCommit } = this.state;

    this.setState({ loading: true });
    merge(mergeStrategy, mergeCommit);
  };

  shouldDisableMergeButton = () => {
    const { mergeStrategy, mergeCommit } = this.state;
    return mergeStrategy !== "FAST_FORWARD_IF_POSSIBLE" && mergeCommit.commitMessage.trim() === "";
  };

  onChangeDeleteSourceBranch = (value: boolean) => {
    this.setState({ mergeCommit: { ...this.state.mergeCommit, shouldDeleteSourceBranch: value } });
  };

  render() {
    const { pullRequest, close, t } = this.props;
    const { mergeStrategy, mergeCommit, loading, loadingDefaultMessage } = this.state;

    const footer = (
      <>
        <Button label={t("scm-review-plugin.showPullRequest.mergeModal.cancel")} action={() => close()} color="grey" />
        <SubmitButton
          label={t("scm-review-plugin.showPullRequest.mergeModal.merge")}
          action={() => this.performMerge()}
          loading={loading}
          disabled={this.shouldDisableMergeButton()}
        />
      </>
    );

    const body = (
      <MergeForm
        selectedStrategy={mergeStrategy}
        selectStrategy={this.selectStrategy}
        strategyLinks={pullRequest._links.merge as Link[]}
        commitMessage={mergeCommit.commitMessage}
        onChangeCommitMessage={this.onChangeCommitMessage}
        shouldDeleteSourceBranch={mergeCommit.shouldDeleteSourceBranch}
        onChangeDeleteSourceBranch={this.onChangeDeleteSourceBranch}
        disabled={loadingDefaultMessage}
      />
    );

    return (
      <Modal
        title={t("scm-review-plugin.showPullRequest.mergeModal.title")}
        active={true}
        body={body}
        closeFunction={close}
        footer={footer}
      />
    );
  }
}

export default withTranslation("plugins")(MergeModal);
