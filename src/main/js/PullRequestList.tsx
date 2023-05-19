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
import React, { FC, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { Repository } from "@scm-manager/ui-types";
import { CreateButton, ErrorPage, LinkPaginator, Loading, Notification, urls } from "@scm-manager/ui-components";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";
import { usePullRequests } from "./pullRequest";
import { PullRequest } from "./types/PullRequest";
import { Redirect, useHistory, useLocation, useRouteMatch } from "react-router-dom";

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

type Props = {
  repository: Repository;
};

const PullRequestList: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const search = urls.getQueryStringFromLocation(location);
  const match: { params: { page: string } } = useRouteMatch();
  const [statusFilter, setStatusFilter] = useState<string>(search || "IN_PROGRESS");
  const history = useHistory();
  const page = useMemo(() => urls.getPageFromMatch(match), [match]);
  const { data, error, isLoading } = usePullRequests(repository, { page, status: statusFilter, pageSize: 10 });

  if (isLoading || !data) {
    return <Loading />;
  }

  const handleStatusChange = (newStatus: string) => {
    setStatusFilter(newStatus);
    history.replace(`1?q=${newStatus}`);
  };

  const renderPullRequestTable = () => {
    return (
      <div className="panel">
        <div className="panel-heading">
          <StatusSelector handleTypeChange={handleStatusChange} status={statusFilter} />
        </div>

        {isLoading ? (
          <Loading />
        ) : (
          <>
            <ScrollingTable className="panel-block">
              <PullRequestTable repository={repository} pullRequests={data._embedded?.pullRequests as PullRequest[]} />
            </ScrollingTable>
            <div className="panel-footer">
              <LinkPaginator collection={data} page={page} filter={statusFilter} />
            </div>
          </>
        )}
      </div>
    );
  };

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  if (error) {
    return (
      <ErrorPage
        title={t("scm-review-plugin.pullRequests.errorTitle")}
        subtitle={t("scm-review-plugin.pullRequests.errorSubtitle")}
        error={error}
      />
    );
  }

  const url = `/repo/${repository.namespace}/${repository.name}/pull-requests`;

  if (data && data.pageTotal < page && page > 1) {
    return <Redirect to={`${url}/${data.pageTotal}`} />;
  }

  const createButton = data?._links?.create ? (
    <CreateButton label={t("scm-review-plugin.pullRequests.createButton")} link={`${url}/add/changesets/`} />
  ) : null;

  return (
    <>
      {renderPullRequestTable()}
      {createButton}
    </>
  );
};

export default PullRequestList;
