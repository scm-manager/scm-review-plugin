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
import { DateFromNow, SmallLoadingSpinner } from "@scm-manager/ui-components";
import { Button, Icon, LinkButton } from "@scm-manager/ui-core";
import styled from "styled-components";
import { Link } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import { useDeletePullRequestBanner } from "./pullRequest";
import { Banner } from "./types/PullRequest";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  banner: Banner;
  createPullRequestLocation: string;
};

// noinspection CssUnresolvedCustomProperty
const StyledBanner = styled.div`
  width: 100%;
  border: 1px solid var(--scm-info-color);
  color: var(--scm-info-color);
  margin-bottom: 0.75rem;
  padding: 0.75rem;
  justify-content: space-between;
  display: flex;
  align-items: center;
  border-radius: 0.5rem;
`;

const PullRequestBanner: FC<Props> = ({ repository, banner, createPullRequestLocation }) => {
  const { mutate, isLoading } = useDeletePullRequestBanner(repository)
  const [t] = useTranslation("plugins");

  return (
    <StyledBanner>
      <div>
        <Icon>code-branch</Icon>
        <Trans
          t={t}
          i18nKey="scm-review-plugin.pullRequests.banner"
          values={{ branch: banner.branch }}
          components={[<DateFromNow date={banner.pushedAt} />]}
        />
      </div>
      <div className="is-flex is-align-items-center">
        <LinkButton to={createPullRequestLocation} variant="info">
          {t("scm-review-plugin.pullRequests.createButton")}
        </LinkButton>
        {isLoading ? <SmallLoadingSpinner className="ml-4"/> : <button
          className="tag is-delete flex-shrink-0 is-clickable is-rounded ml-4"
          onClick={() => mutate(banner)}
          aria-label={t("scm-review-plugin.pullRequests.closeBanner")}
        />}
      </div>
    </StyledBanner>
  );
};

export default PullRequestBanner;
