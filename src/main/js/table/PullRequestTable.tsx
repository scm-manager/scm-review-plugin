/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import styled from "styled-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { Column, comparators, DateFromNow, Table, Tag, TextColumn } from "@scm-manager/ui-components";
import { Link } from "react-router-dom";
import ReviewerIcon from "./ReviewerIcon";
import PullRequestStatusTag from "../PullRequestStatusTag";
import { binder } from "@scm-manager/ui-extensions";
import { PullRequestTableColumn } from "../types/ExtensionPoints";
import PullRequestStatusColumn from "../workflow/PullRequestStatusColumn";

type Props = WithTranslation & {
  repository: Repository;
  pullRequests: PullRequest[];
};

const StyledTable = styled(Table)`
  td {
    word-break: break-all;
  }
`;

const MobileHiddenColumn = styled(Column).attrs(() => ({
  className: "is-hidden-mobile"
}))``;

class PullRequestTable extends React.Component<Props> {
  to = (pullRequest: PullRequest) => {
    const { repository } = this.props;
    return `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`;
  };

  todoTag = (pullRequest: PullRequest) => {
    const { t } = this.props;
    const todos = pullRequest.tasks?.todo;
    if (!todos) {
      return null;
    }
    return (
      <Tag
        className="ml-2"
        label={`${todos}`}
        title={t("scm-review-plugin.pullRequest.tasks.todo", { count: todos })}
      />
    );
  };

  render() {
    const { repository, pullRequests, t } = this.props;
    const baseColumns = [
      <Column
        header={t("scm-review-plugin.pullRequest.title")}
        createComparator={() => comparators.byKey("title")}
        ascendingIcon="sort-alpha-down-alt"
        descendingIcon="sort-alpha-down"
      >
        {(row: PullRequest) => <Link to={this.to(row)}>{row.title}</Link>}
      </Column>,
      <Column header="">{(row: PullRequest) => <>{this.todoTag(row)}</>}</Column>,
      <TextColumn header={t("scm-review-plugin.pullRequest.sourceBranch")} dataKey="source" />,
      <TextColumn header={t("scm-review-plugin.pullRequest.targetBranch")} dataKey="target" />,
      <MobileHiddenColumn
        header={t("scm-review-plugin.pullRequest.author")}
        createComparator={() => comparators.byNestedKeys("author", "displayName")}
        ascendingIcon="sort-alpha-down-alt"
        descendingIcon="sort-alpha-down"
      >
        {(row: PullRequest) => <p>{row.author?.displayName}</p>}
      </MobileHiddenColumn>,
      <MobileHiddenColumn
        header={t("scm-review-plugin.pullRequest.date")}
        createComparator={() => comparators.byKey("creationDate")}
        ascendingIcon="sort-amount-down-alt"
        descendingIcon="sort-amount-down"
      >
        {(row: PullRequest) => (row.creationDate ? <DateFromNow date={row.creationDate} /> : "")}
      </MobileHiddenColumn>,
      <MobileHiddenColumn header="">
        {(row: PullRequest) => <ReviewerIcon reviewers={row.reviewer} />}
      </MobileHiddenColumn>,
      <MobileHiddenColumn
        header={t("scm-review-plugin.pullRequest.status")}
        createComparator={() => comparators.byKey("status")}
      >
        {(row: PullRequest) => <PullRequestStatusTag status={row.status} emergencyMerged={row.emergencyMerged} />}
      </MobileHiddenColumn>,
      <MobileHiddenColumn header={t("scm-review-plugin.workflow.globalConfig.title")}>
        {(row: PullRequest) => <PullRequestStatusColumn pullRequest={row} repository={repository} />}
      </MobileHiddenColumn>
    ];
    const additionalColumns = binder.getExtensions<PullRequestTableColumn>("pull-requests.table.column").map(ext =>
      ext({
        repository,
        t
      })
    );
    const columns = baseColumns.concat(...additionalColumns);
    return (
      <StyledTable data={pullRequests} emptyMessage={t("scm-review-plugin.noRequests")}>
        {columns}
      </StyledTable>
    );
  }
}

export default withTranslation("plugins")(PullRequestTable);
