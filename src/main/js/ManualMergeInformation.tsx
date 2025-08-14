/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
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
