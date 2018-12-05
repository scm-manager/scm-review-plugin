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
  render() {
    const { pullRequests, t } = this.props;
    return (
      <table className="table is-hoverable is-fullwidth">
        <thead>
        <tr>
          <th className="is-hidden-mobile">{t("scm-review-plugin.pull-request.title")}</th>
          <th>{t("scm-review-plugin.pull-request.sourceBranch")}</th>
          <th>{t("scm-review-plugin.pull-request.targetBranch")}</th>
          <th>{t("scm-review-plugin.pull-request.author")}</th>
          <th className="is-hidden-mobile">{t("scm-review-plugin.pull-request.date")}</th>
          <th className="is-hidden-mobile">{t("scm-review-plugin.pull-request.status")}</th>
        </tr>
        </thead>
        <tbody>
        {pullRequests.map((pullRequest, index) => {
          return <PullRequestRow key={index} pullRequest={pullRequest}/>;
        })}
        </tbody>
      </table>
    );
  }
}

export default translate("plugins")(PullRequestTable);
