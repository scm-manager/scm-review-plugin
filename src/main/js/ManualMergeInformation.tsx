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
import { Repository } from "@scm-manager/ui-types";
import { Modal } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import { PullRequest } from "./types/PullRequest";

type Props = WithTranslation & {
  repository: Repository;
  showMergeInformation: boolean;
  pullRequest: PullRequest;
  onClose: () => void;
};

class ManualMergeInformation extends React.Component<Props> {
  render() {
    const { showMergeInformation, repository, pullRequest, onClose, t } = this.props;

    const target = pullRequest.target;
    const source = pullRequest.source;

    if (!showMergeInformation) {
      return null;
    }

    const body = (
      <div className="content">
        <p>
          <em>{t("scm-review-plugin.showPullRequest.mergeButton.conflictTabHint")}</em>
        </p>
        <ExtensionPoint
          name="repos.repository-merge.information"
          renderAll={true}
          props={{
            repository,
            source,
            target
          }}
        />
      </div>
    );

    return (
      <Modal
        title={t("scm-review-plugin.showPullRequest.mergeButton.mergeInformation")}
        closeFunction={() => onClose()}
        body={body}
        active={true}
      />
    );
  }
}

export default withTranslation("plugins")(ManualMergeInformation);
