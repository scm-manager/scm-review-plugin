import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import PullRequestRow from "./PullRequestRow";
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";

type Props = WithTranslation & {
  repository: Repository;
  pullRequests: PullRequest[];
};

class PullRequestTable extends React.Component<Props> {
  renderTable() {
    const { repository, pullRequests, t } = this.props;

    return (
      <>
        <thead>
          <tr>
            <th>{t("scm-review-plugin.pullRequest.title")}</th>
            <th>{t("scm-review-plugin.pullRequest.sourceBranch")}</th>
            <th>{t("scm-review-plugin.pullRequest.targetBranch")}</th>
            <th className="is-hidden-mobile">{t("scm-review-plugin.pullRequest.author")}</th>
            <th className="is-hidden-mobile">{t("scm-review-plugin.pullRequest.date")}</th>
            <th className="is-hidden-mobile">{t("scm-review-plugin.pullRequest.status")}</th>
            <th className="is-hidden-mobile">{t("scm-review-plugin.pullRequest.reviewer")}</th>
          </tr>
        </thead>
        <tbody>
          {pullRequests.map((pullRequest, index) => {
            return <PullRequestRow key={index} repository={repository} pullRequest={pullRequest} />;
          })}
        </tbody>
      </>
    );
  }

  render() {
    const { pullRequests, t } = this.props;

    const info = !(pullRequests && pullRequests.length > 0) ? (
      <div className="notification is-info">{t("scm-review-plugin.noRequests")}</div>
    ) : (
      this.renderTable()
    );
    return <table className="table is-hoverable is-fullwidth">{info}</table>;
  }
}

export default withTranslation("plugins")(PullRequestTable);
