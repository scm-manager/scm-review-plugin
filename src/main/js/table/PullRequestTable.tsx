import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { Table, TextColumn, Column, Tag, DateFromNow } from "@scm-manager/ui-components";
import { Link } from "react-router-dom";

type Props = WithTranslation & {
  repository: Repository;
  pullRequests: PullRequest[];
};

class PullRequestTable extends React.Component<Props> {
  to = (pullRequest: PullRequest) => {
    const { repository } = this.props;
    return `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`;
  };

  render() {
    const { pullRequests, t } = this.props;
    return (
      <Table data={pullRequests} emptyMessage={t("scm-review-plugin.noRequests")}>
        <Column header={t("scm-review-plugin.pullRequest.title")}>
          {(row: any) => <Link to={this.to(row)}>{row.title}</Link>}
        </Column>
        <TextColumn header={t("scm-review-plugin.pullRequest.sourceBranch")} dataKey="source" />
        <TextColumn header={t("scm-review-plugin.pullRequest.targetBranch")} dataKey="target" />
        <Column header={t("scm-review-plugin.pullRequest.author")}>
          {(row: any) => <p>{row.author.displayName}</p>}
        </Column>
        <Column header={t("scm-review-plugin.pullRequest.date")}>
          {(row: any) => (row.creationDate ? <DateFromNow date={row.creationDate} /> : "")}
        </Column>
        <Column header={t("scm-review-plugin.pullRequest.status")}>
          {(row: any) => (
            <Tag
              className="is-medium"
              color={row.status === "MERGED" ? "success" : row.status === "REJECTED" ? "danger" : "light"}
              label={row.status}
            />
          )}
        </Column>
      </Table>
    );
  }
}

export default withTranslation("plugins")(PullRequestTable);
