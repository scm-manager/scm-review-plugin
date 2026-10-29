/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC, useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useLocation, Navigate, useParams } from "react-router";
import styled from "styled-components";
import { ErrorPage, LinkPaginator, Loading, Notification, Subtitle, urls } from "@scm-manager/ui-components";
import { LinkButton, useDocumentTitle, useRepositoryFromParams } from "@scm-manager/ui-core";
import SortSelector from "./table/SortSelector";
import StatusSelector from "./table/StatusSelector";
import { PullRequest } from "./types/PullRequest";
import { usePullRequests } from "./pullRequest";
import PullRequestList from "./PullRequestList";
import {
  PULL_REQUEST_CHANGESET_BASE,
  resolveCreatePullRequest,
  resolvePagedPullRequestOverview,
  resolvePullRequestOverview,
} from "./routes";

const HeaderContainer = styled.div`
  gap: 0.5rem 1rem;
`;

const SortFilterContainer = styled.div`
  flex: 1 1 0;
  gap: 0.5rem 1rem;
`;

const NoGrowLinkButton = styled(LinkButton)`
  flex: 0 0 auto;
`;

const PullRequestsOverview: FC = () => {
  const repository = useRepositoryFromParams();
  const location = useLocation();
  const search = urls.getQueryStringFromLocation(location);
  const sortByQuery = urls.getValueStringFromLocationByKey(location, "sortBy") || "";
  const url = resolvePullRequestOverview(repository);
  const params = useParams<{ page: string }>();
  const [statusFilter, setStatusFilter] = useState<string>(search || "IN_PROGRESS");
  const [sortFilter, setSortFilter] = useState<string>(sortByQuery != "" ? sortByQuery : "LAST_MOD_DESC");
  const navigate = useNavigate();
  const page = parseInt(params.page || "1");
  const { data, error, isLoading } = usePullRequests(repository, {
    page,
    status: statusFilter,
    sortBy: sortFilter,
    pageSize: 10,
  });
  const [t] = useTranslation("plugins");
  useDocumentTitle(
    data?.pageTotal && data.pageTotal > 1 && page
      ? t("scm-review-plugin.pullRequestsWithStatusPageAndNamespaceName", {
          status: t(`scm-review-plugin.pullRequest.titlePart.${statusFilter}`),
          page: page,
          total: data.pageTotal,
          namespace: repository.namespace,
          name: repository.name,
        })
      : t("scm-review-plugin.pullRequestsWithStatusAndNamespaceName", {
          status: t(`scm-review-plugin.pullRequest.titlePart.${statusFilter}`),
          namespace: repository.namespace,
          name: repository.name,
        }),
  );

  const handleQueryChange = useCallback(
    (newStatus: string, newSort: string) => {
      setStatusFilter(newStatus);
      setSortFilter(newSort);
      navigate(`${url}/?q=${newStatus}&sortBy=${newSort}`);
    },
    [navigate, url],
  );

  if (isLoading || !data) {
    return <Loading />;
  }

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

  if (data && data.pageTotal < page && page > 1) {
    //TODO: Fix navigation to page 0 instead of last page
    return <Navigate replace to={resolvePagedPullRequestOverview(repository, page)} />;
  }

  return (
    <>
      <Subtitle subtitle={t("scm-review-plugin.pullRequests.pageSubtitle")} />
      <HeaderContainer className="is-flex is-flex-wrap-wrap is-justify-content-space-between mb-3">
        <SortFilterContainer className="is-flex is-flex-wrap-wrap">
          <StatusSelector
            handleTypeChange={(newStatus) => handleQueryChange(newStatus, sortFilter)}
            status={statusFilter}
          />
          <SortSelector handleTypeChange={(newSort) => handleQueryChange(statusFilter, newSort)} sortBy={sortFilter} />
        </SortFilterContainer>
        {data?._links?.create ? (
          <NoGrowLinkButton variant="primary" to={resolveCreatePullRequest(repository, PULL_REQUEST_CHANGESET_BASE)}>
            {t("scm-review-plugin.pullRequests.createButton")}
          </NoGrowLinkButton>
        ) : null}
      </HeaderContainer>
      <PullRequestList pullRequests={data._embedded?.pullRequests as PullRequest[]} repository={repository} />
      <LinkPaginator collection={data} page={page} filter={statusFilter} baseUrl={url.substring(1)} />
    </>
  );
};

export default PullRequestsOverview;
