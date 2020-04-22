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
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, Tooltip } from "@scm-manager/ui-components";
import ManualMergeInformation from "./ManualMergeInformation";
import { MergeCheck, MergeCommit, PullRequest } from "./types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import MergeModal from "./MergeModal";
import OverrideModal from "./OverrideModal";

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
  showOverrideModal: boolean;
  overrideMessage?: string;
};

class MergeButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      mergeInformation: false,
      showMergeModal: false,
      showOverrideModal: false
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

  toggleOverrideModal = () => {
    this.setState(prevState => ({
      showOverrideModal: !prevState.showOverrideModal
    }));
  };

  toggleMergeModal = () => {
    this.setState(prevState => ({
      showMergeModal: !prevState.showMergeModal
    }));
  };

  mergeWithOverride = (overrideMessage: string) => {
    this.setState({
      overrideMessage,
      showOverrideModal: false,
      showMergeModal: true
    });
  };

  renderButton = () => {
    const { t, loading, mergeCheck } = this.props;

    const color = mergeCheck?.hasConflicts ? "warning" : "primary";
    const checkHints = mergeCheck ? mergeCheck.mergeObstacles.map(o => t(o.key)).join("\n") : "";
    const obstaclesPresent = mergeCheck && mergeCheck.mergeObstacles.length > 0;
    const obstaclesNotOverrideable =
      mergeCheck && mergeCheck.mergeObstacles.filter(obstacle => !obstacle.overrideable).length > 0;
    const disabled = obstaclesPresent && obstaclesNotOverrideable;

    let action;
    if (mergeCheck?.hasConflicts) {
      action = this.showInformation;
    } else if (!disabled && obstaclesPresent) {
      action = this.toggleOverrideModal;
    } else {
      action = this.toggleMergeModal;
    }

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

  addOverrideMessageToMergeCommit = (mergeCommit: MergeCommit) => {
    const { overrideMessage } = this.state;
    if (overrideMessage) {
      return {
        ...mergeCommit,
        overrideMessage: overrideMessage
      };
    } else {
      return mergeCommit;
    }
  };

  render() {
    const { repository, pullRequest, merge } = this.props;
    const { mergeInformation, showOverrideModal, showMergeModal } = this.state;

    if (showOverrideModal) {
      return (
        <OverrideModal
          proceed={(message: string) => this.mergeWithOverride(message)}
          close={this.toggleOverrideModal}
          pullRequest={pullRequest}
        />
      );
    }

    if (showMergeModal) {
      return (
        <MergeModal
          merge={(strategy: string, mergeCommit: MergeCommit) =>
            merge(strategy, this.addOverrideMessageToMergeCommit(mergeCommit))
          }
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
