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
        title={t("scm-review-plugin.show-pull-request.mergeButton.merge-information")}
        closeFunction={() => onClose()}
        body={body}
        active={true}
      />
    );
  }
}

export default withTranslation("plugins")(ManualMergeInformation);
