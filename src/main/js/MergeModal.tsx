/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import { Button, Modal, SubmitButton } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import MergeForm from "./MergeForm";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { Link } from "@scm-manager/ui-types";
import { getDefaultCommitDefaultMessage } from "./pullRequest";

type Props = WithTranslation & {
  close: () => void;
  merge: (strategy: string, mergeCommit: MergeCommit, emergency: boolean) => void;
  pullRequest: PullRequest;
  emergencyMerge: boolean;
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
      mergeStrategy: this.extractFirstMergeStrategy(props.pullRequest._links.merge as Link[]),
      mergeCommit: {
        commitMessage: "",
        shouldDeleteSourceBranch: false,
      },
      defaultCommitMessages: {},
      loading: false,
      loadingDefaultMessage: false,
      messageChanged: false
    };
  }

  extractFirstMergeStrategy: (mergeStrategyLinks: Link[]) => string = mergeStrategyLinks => {
    if (mergeStrategyLinks && mergeStrategyLinks.length > 0 && mergeStrategyLinks[0].name) {
      return mergeStrategyLinks[0].name;
    } else {
      throw new Error("no merge strategies found");
    }
  };

  componentDidMount(): void {
    this.getDefaultMessage();
  }

  getDefaultMessage = () => {
    const { pullRequest } = this.props;
    const { defaultCommitMessages, mergeCommit, mergeStrategy } = this.state;
    if (!!defaultCommitMessages[mergeStrategy]) {
      this.setState({
        mergeCommit: { ...mergeCommit, commitMessage: defaultCommitMessages[mergeStrategy] },
        messageChanged: false
      });
    } else if (pullRequest && pullRequest._links && pullRequest._links.defaultCommitMessage) {
      this.setState({ loadingDefaultMessage: true }, () => {
        getDefaultCommitDefaultMessage(
          (pullRequest._links.defaultCommitMessage as Link).href + "?strategy=" + mergeStrategy
        )
          .then(commitMessage => {
            defaultCommitMessages[mergeStrategy] = commitMessage;
            this.setState({
              defaultCommitMessages: defaultCommitMessages,
              mergeCommit: { ...mergeCommit, commitMessage: commitMessage },
              messageChanged: false,
              loadingDefaultMessage: false
            });
          })
          .catch(error => {
            this.setState({
              mergeCommit: { ...mergeCommit, commitMessage: "" },
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

  performMerge = (emergency: boolean) => {
    const { merge } = this.props;
    const { mergeStrategy, mergeCommit } = this.state;

    this.setState({ loading: true });
    merge(mergeStrategy, mergeCommit, emergency);
  };

  shouldDisableMergeButton = () => {
    const { mergeCommit } = this.state;
    return !mergeCommit.commitMessage || mergeCommit.commitMessage.trim() === "";
  };

  onChangeDeleteSourceBranch = (value: boolean) => {
    this.setState({ mergeCommit: { ...this.state.mergeCommit, shouldDeleteSourceBranch: value } });
  };

  render() {
    const { pullRequest, emergencyMerge, close, t } = this.props;
    const { mergeStrategy, mergeCommit, loading, loadingDefaultMessage } = this.state;

    const footer = (
      <>
        <Button label={t("scm-review-plugin.showPullRequest.mergeModal.cancel")} action={() => close()} color="grey" />
        {emergencyMerge ? (
          <SubmitButton
            icon="exclamation-triangle"
            color="danger"
            label={t("scm-review-plugin.showPullRequest.mergeModal.merge")}
            action={() => this.performMerge(true)}
            loading={loading}
            disabled={this.shouldDisableMergeButton()}
          />
        ) : (
          <SubmitButton
            label={t("scm-review-plugin.showPullRequest.mergeModal.merge")}
            action={() => this.performMerge(false)}
            loading={loading}
            disabled={this.shouldDisableMergeButton()}
          />
        )}
      </>
    );

    const body = (
      <MergeForm
        selectedStrategy={mergeStrategy}
        selectStrategy={this.selectStrategy}
        strategyLinks={pullRequest._links.merge as Link[]}
        commitMessage={mergeCommit.commitMessage}
        onChangeCommitMessage={this.onChangeCommitMessage}
        onResetCommitMessage={this.getDefaultMessage}
        shouldDeleteSourceBranch={mergeCommit.shouldDeleteSourceBranch}
        onChangeDeleteSourceBranch={this.onChangeDeleteSourceBranch}
        loading={loadingDefaultMessage}
      />
    );

    return (
      <Modal
        title={
          emergencyMerge
            ? t("scm-review-plugin.showPullRequest.mergeModal.title.emergencyMerge")
            : t("scm-review-plugin.showPullRequest.mergeModal.title.merge")
        }
        active={true}
        body={body}
        closeFunction={close}
        footer={footer}
      />
    );
  }
}

export default withTranslation("plugins")(MergeModal);
