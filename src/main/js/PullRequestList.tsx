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
import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { Repository } from "@scm-manager/ui-types";
import { CreateButton, ErrorPage, Loading, Notification } from "@scm-manager/ui-components";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";
import { invalidatePullRequests, usePullRequests } from "./pullRequest";

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

type Props = {
  repository: Repository;
};

const PullRequestList: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const [status, setStatus] = useState("OPEN");

  const { data, error, isLoading } = usePullRequests(repository);

  const handleStatusChange = (newStatus: string) => {
    setStatus(newStatus);
    return invalidatePullRequests(repository);
  };

  const renderPullRequestTable = () => {
    return (
      <div className="panel">
        <div className="panel-heading">
          <StatusSelector handleTypeChange={handleStatusChange} status={status ? status : "OPEN"} />
        </div>

        <ScrollingTable className="panel-block">
          <PullRequestTable repository={repository} pullRequests={data!._embedded.pullRequests} />
        </ScrollingTable>
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

  if (!data || isLoading) {
    return <Loading />;
  }

  const to = "pull-requests/add/changesets/";

  const createButton = data._links.create ? (
    <CreateButton label={t("scm-review-plugin.pullRequests.createButton")} link={to} />
  ) : null;

  return (
    <>
      {renderPullRequestTable()}
      {createButton}
    </>
  );
};

export default PullRequestList;
