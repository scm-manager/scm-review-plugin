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
import { Route, Switch, useRouteMatch } from "react-router-dom";
import PullRequestDetails from "./PullRequestDetails";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequest } from "./pullRequest";
import Edit from "./Edit";
import { useTranslation } from "react-i18next";

type Props = {
  repository: Repository;
};

const SinglePullRequest: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch<{ pullRequestNumber?: string }>();
  const { data, error, isLoading } = usePullRequest(repository, match?.params?.pullRequestNumber);

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!data || isLoading) {
    return <Loading />;
  }

  return (
    <Switch>
      <Route path={`${match.url}/edit`} exact>
        <Edit repository={repository} pullRequest={data} />
      </Route>
      <Route path={`${match.url}`}>
        <PullRequestDetails repository={repository} pullRequest={data} />
      </Route>
    </Switch>
  );
};

export default SinglePullRequest;
