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
import { useTranslation } from "react-i18next";
import { Branch, Repository } from "@scm-manager/ui-types";
import { AddButton, ErrorNotification, Loading, SubSubtitle } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import styled from "styled-components";
import { usePullRequests } from "./pullRequest";
import PullRequestList from "./PullRequestList";

type Props = {
  repository: Repository;
  branch: Branch;
};

const HR = styled.hr`
  height: 3px;
`;

const CreatePullRequestButton: FC<Props> = ({ repository, branch }) => {
  const [t] = useTranslation("plugins");
  const { data, error, isLoading } = usePullRequests(repository, { source: branch.name, pageSize: 999 });

  if (!repository._links.pullRequest) {
    return null;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!data || isLoading) {
    return <Loading />;
  }

  const matchingPullRequests = (data._embedded?.pullRequests as PullRequest[]).filter(
    (pr: PullRequest) => pr.source === branch.name,
  );

  let existing = null;
  if (matchingPullRequests.length > 0) {
    existing = <PullRequestList repository={repository} pullRequests={matchingPullRequests} />;
  }

  return (
    <div>
      <HR />
      <SubSubtitle>{t("scm-review-plugin.branch.header")}</SubSubtitle>
      {existing}
      <AddButton
        label={t("scm-review-plugin.branch.createPullRequest")}
        link={`/repo/${repository.namespace}/${
          repository.name
        }/pull-requests/add/changesets?source=${encodeURIComponent(branch.name)}`}
      />
    </div>
  );
};

export default CreatePullRequestButton;
