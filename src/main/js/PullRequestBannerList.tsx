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
import { Repository } from "@scm-manager/ui-types";
import PullRequestBanner from "./PullRequestBanner";
import { Loading } from "@scm-manager/ui-core";
import { usePullRequestBanner } from "./pullRequest";
import { Banner } from "./types/PullRequest";

type Props = {
  repository: Repository;
};

const PullRequestBannerList: FC<Props> = ({ repository }) => {
  const { data } = usePullRequestBanner(repository);

  if (!data) {
    return null;
  }

  return (
    <>
      {data.map((banner: Banner) => {
        return (
          <PullRequestBanner
            key={banner.branch}
            repository={repository}
            banner={banner}
            createPullRequestLocation={`/repo/${repository.namespace}/${repository.name}/pull-requests/add/changesets?source=${encodeURIComponent(banner.branch)}`}
          />
        );
      })}
    </>
  );
};

export default PullRequestBannerList;
