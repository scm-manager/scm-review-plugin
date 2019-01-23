// @flow
import React from "react";
import { translate } from "react-i18next";
import PullRequestRow from "./PullRequestRow";
import type { PullRequest } from "./../types/PullRequest";
import injectSheet from "react-jss";
import classNames from "classnames";

type Props = {
  t: string => string,
  pullRequests: PullRequest[]
};

const styles = {
  tableScrolling: {
    display: "block",
    overflowX: "auto"
  }
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
    const { pullRequests, t, classes } = this.props;

    const info = !(pullRequests && pullRequests.length > 0) ? (
      <div className="notification is-info">
        {t("scm-review-plugin.no-requests")}
      </div>
    ) : (
      this.renderTable()
    );
    return (
      <table
        className={classNames(
          "table is-hoverable is-fullwidth",
          classes.tableScrolling
        )}
      >
        {info}
      </table>
    );
  }
}

export default injectSheet(styles)(translate("plugins")(PullRequestTable));
