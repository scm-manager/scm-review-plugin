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

import React, { FC, useEffect, useState } from "react";
import { Navigate, useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequestChangesets } from "./pullRequest";
import {
  ChangesetList,
  ErrorNotification,
  LinkPaginator,
  Loading,
  NotFoundError,
  Notification,
} from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { usePullRequestMatchRoute } from "./routes";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  shouldFetchChangesets?: boolean;
};

const Changesets: FC<Props> = ({ repository, pullRequest, shouldFetchChangesets = true }) => {
  const [t] = useTranslation("plugins");
  const params = useParams<{ page: string }>();
  const [page, setPage] = useState(1);
  const match = usePullRequestMatchRoute();
  const url = match.pathnameBase;

  const { data: changesets, error, isLoading } = usePullRequestChangesets(repository, pullRequest, page);

  useEffect(() => {
    if (params.page) {
      setPage(parseInt(params.page, 10));
    }
  }, [params]);

  const renderPaginator = () => {
    if (changesets && changesets.pageTotal > 1) {
      return (
        <div className="panel-footer">
          <LinkPaginator collection={changesets} page={page} baseUrl={url.substring(1)} />
        </div>
      );
    }
    return null;
  };

  if (error instanceof NotFoundError || !shouldFetchChangesets) {
    return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
  } else if (error) {
    return <ErrorNotification error={error} />;
  } else if (isLoading) {
    return <Loading />;
  } else if (changesets && changesets.pageTotal < page && page > 1) {
    return <Navigate to={`${url}/${changesets.pageTotal}`} />;
  } else if (changesets && changesets._embedded && changesets._embedded.changesets) {
    if (changesets._embedded.changesets?.length !== 0) {
      return (
        <div className="panel">
          <div className="panel-block">
            <ChangesetList repository={repository} changesets={changesets._embedded.changesets} />
          </div>
          {renderPaginator()}
        </div>
      );
    } else {
      return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
    }
  }
  return null;
};

export default Changesets;
