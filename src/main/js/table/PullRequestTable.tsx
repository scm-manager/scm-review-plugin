import React from "react";
import styled from "styled-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { Column, DateFromNow, Table, Tag, TextColumn } from "@scm-manager/ui-components";
import { Link } from "react-router-dom";
import ReviewerIcon from "./ReviewerIcon";
import comparators from "./comparators";

type Props = WithTranslation & {
  repository: Repository;
  pullRequests: PullRequest[];
};

const MobileHiddenColumn = styled(Column).attrs(() => {
  className: "is-hidden-mobile";
})``;

const TodoTag = styled(Tag).attrs(props => ({}))`
  margin-left: 0.5em;
`;

class PullRequestTable extends React.Component<Props> {
  to = (pullRequest: PullRequest) => {
    const { repository } = this.props;
    return `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`;
  };

  todoTag = (pullRequest: PullRequest) => {
    const { t } = this.props;
    const todos = pullRequest.tasks.todo;
    if (todos === 0) {
      return null;
    }
    return <TodoTag label={`${todos}`} title={t("scm-review-plugin.pullRequest.tasks.todo", { count: todos })} />;
  };

  render() {
    const { pullRequests, t } = this.props;
    return (
      <Table data={pullRequests} emptyMessage={t("scm-review-plugin.noRequests")}>
        <Column
          header={t("scm-review-plugin.pullRequest.title")}
          createComparator={() => comparators.byKey("title")}
          ascendingIcon="sort-alpha-down-alt"
          descendingIcon="sort-alpha-down"
        >
          {(row: any) => <Link to={this.to(row)}>{row.title}</Link>}
        </Column>
        <Column header="">{(row: any) => <>{this.todoTag(row)}</>}</Column>
        <TextColumn header={t("scm-review-plugin.pullRequest.sourceBranch")} dataKey="source" />
        <TextColumn header={t("scm-review-plugin.pullRequest.targetBranch")} dataKey="target" />
        <MobileHiddenColumn
          header={t("scm-review-plugin.pullRequest.author")}
          createComparator={() => comparators.byNestedKeys("author", "displayName")}
          ascendingIcon="sort-alpha-down-alt"
          descendingIcon="sort-alpha-down"
        >
          {(row: any) => <p>{row.author.displayName}</p>}
        </MobileHiddenColumn>
        <MobileHiddenColumn
          header={t("scm-review-plugin.pullRequest.date")}
          createComparator={() => comparators.byKey("creationDate")}
          ascendingIcon="sort-amount-down-alt"
          descendingIcon="sort-amount-down"
        >
          {(row: any) => (row.creationDate ? <DateFromNow date={row.creationDate} /> : "")}
        </MobileHiddenColumn>
        <MobileHiddenColumn header="">{(row: any) => <ReviewerIcon reviewers={row.reviewer} />}</MobileHiddenColumn>
        <MobileHiddenColumn
          header={t("scm-review-plugin.pullRequest.status")}
          createComparator={() => comparators.byKey("status")}
        >
          {(row: any) => (
            <Tag
              className="is-medium"
              color={row.status === "MERGED" ? "success" : row.status === "REJECTED" ? "danger" : "light"}
              label={row.status}
            />
          )}
        </MobileHiddenColumn>
      </Table>
    );
  }
}

export default withTranslation("plugins")(PullRequestTable);
