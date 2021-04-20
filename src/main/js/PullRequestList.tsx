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
import React, { FC, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { Repository, Link } from "@scm-manager/ui-types";
import { CreateButton, ErrorPage, Loading, Notification } from "@scm-manager/ui-components";
import { PullRequestCollection } from "./types/PullRequest";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";
import { getPullRequests } from "./pullRequest";

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

type Props = {
  repository: Repository;
};

const PullRequestList: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const [pullRequests, setPullRequests] = useState<PullRequestCollection | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [isLoading, setLoading] = useState(true);
  const [status, setStatus] = useState("OPEN");

  useEffect(() => {
    if (repository._links.pullRequest) {
      const url = (repository._links.pullRequest as Link).href;
      updatePullRequests(url);
    } else {
      setLoading(false);
    }
  }, []);

  const updatePullRequests = (url: string) => {
    getPullRequests(url)
      .then(pullRequests => {
        setPullRequests(pullRequests);
        setLoading(false);
      })
      .catch(error => {
        setError(error);
        setLoading(false);
      });
  };

  const handleStatusChange = (status: string) => {
    setStatus(status);
    const url = `${(repository._links.pullRequest as Link).href}?status=${status}`;
    updatePullRequests(url);
  };

  const renderPullRequestTable = () => {
    return (
      <div className="panel">
        <div className="panel-heading">
          <StatusSelector handleTypeChange={handleStatusChange} status={status ? status : "OPEN"} />
        </div>

        <ScrollingTable className="panel-block">
          <PullRequestTable
            repository={repository}
            pullRequests={pullRequests && pullRequests._embedded.pullRequests}
          />
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

  if (!pullRequests || isLoading) {
    return <Loading />;
  }

  const to = "pull-requests/add/changesets/";

  const createButton = pullRequests._links.create ? (
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
