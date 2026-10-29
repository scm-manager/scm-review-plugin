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

import React, { FC } from "react";
import { ErrorNotification, Loading, Notification } from "@scm-manager/ui-components";
import PullRequestDetails from "./PullRequestDetails";
import { usePullRequest } from "./pullRequest";
import { useTranslation } from "react-i18next";
import { useRepositoryFromParams } from "@scm-manager/ui-core";
import { useParams } from "react-router";
import { useLayoutWide } from "@scm-manager/ui-core";

const PullRequestDetailsWrapper: FC = () => {
  const [t] = useTranslation("plugins");
  const repository = useRepositoryFromParams();
  const match = useParams<{ pullRequestId: string }>();
  const { data, error, isLoading } = usePullRequest(repository, match.pullRequestId);
  useLayoutWide();

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!data || isLoading) {
    return <Loading />;
  }

  return <PullRequestDetails repository={repository} pullRequest={data} />;
};

export default PullRequestDetailsWrapper;
