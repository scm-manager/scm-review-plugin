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
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequestChanges } from "../pullRequest";
import { ErrorNotification, Loading } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import ChangeContainer from "./ChangeContainer";

type Props = {
  pullRequest: PullRequest;
  repository: Repository;
};

const RootChangesContainer: FC<Props> = ({ pullRequest, repository }) => {
  const { t } = useTranslation("plugins");
  const { error, isLoading, data } = usePullRequestChanges(repository, pullRequest);

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (data?.length === 0) {
    return <p>{t("scm-review-plugin.pullRequest.changes.noChanges")}</p>;
  }

  return (
    <div>
      <ul>
        {data
          ? data.map(change => (
              <li>
                <ChangeContainer change={change} repository={repository} />
              </li>
            ))
          : null}
      </ul>
    </div>
  );
};

export default RootChangesContainer;
