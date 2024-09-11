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

import React, { FC, useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Repository } from "@scm-manager/ui-types";
import { ErrorPage, LinkPaginator, Loading, Notification, Subtitle, urls } from "@scm-manager/ui-components";
import StatusSelector from "./table/StatusSelector";
import { usePullRequests } from "./pullRequest";
import { PullRequest } from "./types/PullRequest";
import { Redirect, useHistory, useLocation, useRouteMatch } from "react-router-dom";
import PullRequestList from "./PullRequestList";
import { LinkButton } from "@scm-manager/ui-buttons";
import SortSelector from "./table/SortSelector";
import styled from "styled-components";

type Props = {
  repository: Repository;
};

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

const PullRequestsOverview: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const search = urls.getQueryStringFromLocation(location);
  const sortByQuery = urls.getValueStringFromLocationByKey(location, "sortBy") || "";
  const match: { params: { page: string } } = useRouteMatch();
  const [statusFilter, setStatusFilter] = useState<string>(search || "IN_PROGRESS");
  const [sortFilter, setSortFilter] = useState<string>(sortByQuery != "" ? sortByQuery : "LAST_MOD_DESC");
  const history = useHistory();
  const page = useMemo(() => urls.getPageFromMatch(match), [match]);
  const { data, error, isLoading } = usePullRequests(repository, {
    page,
    status: statusFilter,
    sortBy: sortFilter,
    pageSize: 10
  });
  const handleQueryChange = useCallback(
    (newStatus: string, newSort: string) => {
      setStatusFilter(newStatus);
      setSortFilter(newSort);
      history.push(`1?q=${newStatus}&sortBy=${newSort}`);
    },
    [history]
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

  const url = `/repo/${repository.namespace}/${repository.name}/pull-requests`;

  if (data && data.pageTotal < page && page > 1) {
    return <Redirect to={`${url}/${data.pageTotal}`} />;
  }

  return (
    <>
      <Subtitle subtitle={t("scm-review-plugin.pullRequests.pageSubtitle")} />
      <HeaderContainer className="is-flex is-flex-wrap-wrap is-justify-content-space-between mb-3">
        <SortFilterContainer className="is-flex is-flex-wrap-wrap">
          <StatusSelector
            handleTypeChange={newStatus => handleQueryChange(newStatus, sortFilter)}
            status={statusFilter}
          />
          <SortSelector handleTypeChange={newSort => handleQueryChange(statusFilter, newSort)} sortBy={sortFilter} />
        </SortFilterContainer>
        {data?._links?.create ? (
          <NoGrowLinkButton variant="primary" to={`${url}/add/changesets/`}>
            {t("scm-review-plugin.pullRequests.createButton")}
          </NoGrowLinkButton>
        ) : null}
      </HeaderContainer>
      <PullRequestList pullRequests={data._embedded?.pullRequests as PullRequest[]} repository={repository} />
      <LinkPaginator collection={data} page={page} filter={statusFilter} />
    </>
  );
};

export default PullRequestsOverview;
