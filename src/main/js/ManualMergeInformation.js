//@flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
import { Modal } from "@scm-manager/ui-components";
import { translate } from "react-i18next";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import type { PullRequest } from "./types/PullRequest";

type Props = {
  repository: Repository,
  showMergeInformation: boolean,
  pullRequest: PullRequest,
  t: string => string,
  onClose: () => void
};

class ManualMergeInformation extends React.Component<Props> {
  render() {
    const {
      showMergeInformation,
      repository,
      pullRequest,
      onClose,
      t
    } = this.props;

    const target = pullRequest.target;
    const source = pullRequest.source;

    if (!showMergeInformation) {
      return null;
    }

    const closeButton=(
      <button
        className="delete"
        aria-label="close"
        onClick={() => onClose()}
      />
    );

    const body = (
      <div className="content">
        <ExtensionPoint
          name="repos.repository-merge.information"
          renderAll={true}
          props={{ repository, source, target }}
        />
      </div>
    );


    return (
      <Modal title={t(
        "scm-review-plugin.show-pull-request.mergeButton.merge-information"
      )} closeButton={closeButton} body={body} active={true}/>
    );
  }
}

export default translate("plugins")(ManualMergeInformation);
