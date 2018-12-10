// @flow
import React from "react";
import type { Repository } from "@scm-manager/ui-types";
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
    return (
      <div className="modal is-active">
        <div className="modal-background" />
        <div className="modal-card">
          <header className="modal-card-head">
            <p className="modal-card-title">Modal title</p>
            <button
              className="delete"
              aria-label="close"
              onClick={() => onClose()}
            />
          </header>
          <section className="modal-card-body">
            <div className="content">
              <ExtensionPoint
                name="repos.repository-merge.information"
                renderAll={true}
                props={{ repository, source, target }}
              />
            </div>
          </section>
        </div>
      </div>
    );
  }
}

export default translate("plugins")(ManualMergeInformation);
