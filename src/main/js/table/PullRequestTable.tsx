import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { Table, TextColumn, Column, Tag, DateFromNow } from "@scm-manager/ui-components";
import { Link } from "react-router-dom";
import ReviewerIcon from "./ReviewerIcon";

type Props = WithTranslation & {
  repository: Repository;
  pullRequests: PullRequest[];
};

class PullRequestTable extends React.Component<Props> {
  to = (pullRequest: PullRequest) => {
    const { repository } = this.props;
    return `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`;
  };

  createComparator = (key: string) => {
    return (a: any, b: any) => {
      if (a[key] < b[key]) {
        return -1;
      } else if (a[key] > b[key]) {
        return 1;
      } else {
        return 0;
      }
    };
  };

  render() {
    const { pullRequests, t } = this.props;
    return (
      <Table data={pullRequests} emptyMessage={t("scm-review-plugin.noRequests")}>
        <Column
          header={t("scm-review-plugin.pullRequest.title")}
          createComparator={() => this.createComparator("title")}
          ascendingIcon="sort-alpha-down-alt"
          descendingIcon="sort-alpha-down"
        >
          {(row: any) => <Link to={this.to(row)}>{row.title}</Link>}
        </Column>
        <TextColumn header={t("scm-review-plugin.pullRequest.sourceBranch")} dataKey="source" />
        <TextColumn header={t("scm-review-plugin.pullRequest.targetBranch")} dataKey="target" />
        <Column
          header={t("scm-review-plugin.pullRequest.author")}
          createComparator={() => this.createComparator("author")}
          ascendingIcon="sort-alpha-down-alt"
          descendingIcon="sort-alpha-down"
        >
          {(row: any) => <p>{row.author.displayName}</p>}
        </Column>
        <Column
          header={t("scm-review-plugin.pullRequest.date")}
          createComparator={() => this.createComparator("creationDate")}
          ascendingIcon="sort-amount-down-alt"
          descendingIcon="sort-amount-down"
        >
          {(row: any) => (row.creationDate ? <DateFromNow date={row.creationDate} /> : "")}
        </Column>
        <Column header={t("scm-review-plugin.pullRequest.reviewer")}>
          {(row: any) => <ReviewerIcon reviewers={row.reviewer}/>}
        </Column>
        <Column
          header={t("scm-review-plugin.pullRequest.status")}
          createComparator={() => this.createComparator("status")}
        >
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
