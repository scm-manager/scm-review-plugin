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
import {WithTranslation, withTranslation} from "react-i18next";
import {Link} from "@scm-manager/ui-types";
import MergeStrategies from "./MergeStrategies";
import {Button, Checkbox, Textarea} from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = WithTranslation & {
  strategyLinks: Link[];
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  commitMessage: string;
  commitMessageDisabled?: boolean;
  commitMessageHint?: string;
  onChangeCommitMessage: (message: string) => void;
  onResetCommitMessage: () => void;
  shouldDeleteSourceBranch: boolean;
  onChangeDeleteSourceBranch: (value: boolean) => void;
  loading: boolean;
};

const CommitMessageInfo = styled.div`
  margin-bottom: 1em;
`;

class MergeForm extends React.Component<Props> {
  isCommitMessageDisabled = () => {
    return this.props.loading;
  };

  isCommitMessageVisible = () => {
    return !this.props.commitMessageDisabled;
  }

  isShowMessageHint = () => {
    return !!this.props.commitMessageHint;
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
      onResetCommitMessage,
      t,
      commitMessageHint
    } = this.props;

    const commitMessageElement = <>
      <Textarea
        placeholder={t("scm-review-plugin.showPullRequest.mergeModal.commitMessage")}
        disabled={this.isCommitMessageDisabled()}
        value={commitMessage}
        onChange={onChangeCommitMessage}
      />
      {this.isShowMessageHint() && (
        <CommitMessageInfo className="is-size-7">
            <span className="icon is-small has-text-info">
              <i className="fas fa-info-circle"/>
            </span>{" "}
          <span>{t('scm-review-plugin.showPullRequest.mergeModal.commitMessageHint.' + commitMessageHint)}</span>
        </CommitMessageInfo>
      )}
      <Button label={t("scm-review-plugin.showPullRequest.mergeModal.resetMessage")} action={onResetCommitMessage}/>
      <hr/>
    </>

    return (
      <>
        <MergeStrategies
          strategyLinks={strategyLinks}
          selectedStrategy={selectedStrategy}
          selectStrategy={selectStrategy}
        />
        <hr/>
        {this.isCommitMessageVisible() && commitMessageElement}
        <Checkbox
          label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.flag")}
          checked={shouldDeleteSourceBranch}
          helpText={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
          onChange={onChangeDeleteSourceBranch}
        />
      </>
    );
  }
}

export default withTranslation("plugins")(MergeForm);
