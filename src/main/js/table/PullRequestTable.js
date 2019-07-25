// @flow
import React from "react";
import { translate } from "react-i18next";
import PullRequestRow from "./PullRequestRow";
import type { PullRequest } from "./../types/PullRequest";

type Props = {
  t: string => string,
  pullRequests: PullRequest[]
};

class PullRequestTable extends React.Component<Props> {
  renderTable() {
    const { pullRequests, t } = this.props;

    return (
      <>
        <thead>
          <tr>
            <th className="is-hidden-mobile">
              {t("scm-review-plugin.pull-request.title")}
            </th>
            <th>{t("scm-review-plugin.pull-request.sourceBranch")}</th>
            <th>{t("scm-review-plugin.pull-request.targetBranch")}</th>
            <th>{t("scm-review-plugin.pull-request.author")}</th>
            <th className="is-hidden-mobile">
              {t("scm-review-plugin.pull-request.date")}
            </th>
            <th className="is-hidden-mobile">
              {t("scm-review-plugin.pull-request.status")}
            </th>
          </tr>
        </thead>
        <tbody>
          {pullRequests.map((pullRequest, index) => {
            return <PullRequestRow key={index} pullRequest={pullRequest} />;
          })}
        </tbody>
      </>
    );
  }

  render() {
    const { pullRequests, t } = this.props;

    const info = !(pullRequests && pullRequests.length > 0) ? (
      <div className="notification is-info">
        {t("scm-review-plugin.no-requests")}
      </div>
    ) : (
      this.renderTable()
    );
    return <table className="table is-hoverable is-fullwidth">{info}</table>;
  }
}

export default translate("plugins")(PullRequestTable);
