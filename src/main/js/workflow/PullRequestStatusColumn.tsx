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
import { useStatusbar } from "./useStatusbar";
import { SmallLoadingSpinner } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import StatusIcon, { getColor, getIcon, getTitle } from "./StatusIcon";
import ModalRow from "./ModalRow";
import { Popover } from "@scm-manager/ui-overlays";
import { useTranslation } from "react-i18next";
import { Card } from "@scm-manager/ui-layout";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const PullRequestStatusColumn: FC<Props> = ({ pullRequest, repository }) => {
  const [t] = useTranslation("plugins");
  const { data, error, isLoading } = useStatusbar(repository, pullRequest);

  if (isLoading) {
    return <SmallLoadingSpinner className="is-inline-block" />;
  }

  if (error || !data) {
    return null;
  }

  const title = (
    <h1 className="has-text-weight-bold is-size-5">{t("scm-review-plugin.pullRequests.details.workflow")}</h1>
  );

  return (
    <Popover
      trigger={
        <Card.Details.ButtonDetail
          aria-label={t("scm-review-plugin.pullRequests.aria.workflow.label", {
            status: t(`scm-review-plugin.pullRequests.aria.workflow.status.${getTitle(data.results)}`)
          })}
        >
          <Card.Details.Detail.Label aria-hidden>
            {t("scm-review-plugin.pullRequests.details.workflow")}
          </Card.Details.Detail.Label>
          <StatusIcon color={getColor(data.results)} icon={getIcon(data.results)} size="lg" />
        </Card.Details.ButtonDetail>
      }
      title={title}
    >
      <ul>
        {data.results.map(r => (
          <ModalRow key={r.rule} result={r} />
        ))}
      </ul>
    </Popover>
  );
};

export default PullRequestStatusColumn;
