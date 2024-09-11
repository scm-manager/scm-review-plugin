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

import { PullRequest, Reviewer } from "./types/PullRequest";
import { Card, CardListBox } from "@scm-manager/ui-layout";
import { Link } from "react-router-dom";
import { DateFromNow, Notification } from "@scm-manager/ui-components";
import { PullRequestListDetailExtension } from "./types/ExtensionPoints";
import classNames from "classnames";
import { evaluateTagColor } from "./pullRequest";
import React, { FC } from "react";
import { Repository } from "@scm-manager/ui-types";
import styled from "styled-components";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import { Trans, useTranslation } from "react-i18next";
import PullRequestStatusColumn from "./workflow/PullRequestStatusColumn";
import useEngineConfig from "./workflow/useEngineConfig";
import { Tooltip } from "@scm-manager/ui-overlays";

const SubtitleRow = styled(CardListBox.Card.Row)`
  white-space: pre;
`;

const createTooltipMessage = (reviewers: Reviewer[]) => {
  return reviewers.map(({ displayName, approved }) => `- ${displayName}${approved ? " ✔" : ""}`).join("\n");
};

const ReviewersDetail: FC<{ pullRequest: PullRequest }> = ({ pullRequest }) => {
  const [t] = useTranslation("plugins");
  const content = (
    <Card.Details.Detail className={classNames({ "is-relative": pullRequest.reviewer?.length })}>
      <Card.Details.Detail.Label>{t("scm-review-plugin.pullRequests.details.reviewers")}</Card.Details.Detail.Label>
      <Card.Details.Detail.Tag>
        {pullRequest.reviewer?.reduce((p, { approved }) => (approved ? p + 1 : p), 0)}/
        {pullRequest.reviewer?.length ?? 0}
      </Card.Details.Detail.Tag>
    </Card.Details.Detail>
  );
  if (pullRequest.reviewer?.length) {
    return (
      <Tooltip
        message={t("scm-review-plugin.pullRequest.reviewer") + ":\n" + createTooltipMessage(pullRequest.reviewer)}
      >
        {content}
      </Tooltip>
    );
  }
  return content;
};

type Props = {
  repository: Repository;
  pullRequests?: PullRequest[];
};

const PullRequestList: FC<Props> = ({ pullRequests, repository }) => {
  const [t] = useTranslation("plugins");
  const { data, error } = useEngineConfig(repository);

  if (!pullRequests?.length) {
    return <Notification type="info">{t("scm-review-plugin.noRequests")}</Notification>;
  }

  return (
    <CardListBox className="p-2">
      {pullRequests.map(pullRequest => (
        <CardListBox.Card key={pullRequest.id}>
          <Card.Row className="is-flex">
            <Card.Title>
              <Link to={`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`}>
                {pullRequest.title}
              </Link> <span className="has-text-secondary ml-1" aria-label={t("scm-review-plugin.pullRequests.aria.id")}>
              <span aria-hidden>#</span>
              {pullRequest.id}
            </span>
            </Card.Title>
          </Card.Row>
          <SubtitleRow className="is-flex is-flex-wrap-wrap is-size-7 has-text-secondary">
            <Trans
              t={t}
              i18nKey="scm-review-plugin.pullRequests.subtitle"
              values={{
                author: pullRequest.author?.displayName,
                source: pullRequest.source,
                target: pullRequest.target
              }}
              components={{
                highlight: <span className="has-text-default" />,
                date: <DateFromNow className="is-relative" date={pullRequest.creationDate} />,
                space: <span />
              }}
            />
          </SubtitleRow>
          <Card.Row className="is-flex is-align-items-center is-justify-content-space-between is-size-7">
            <Card.Details>
              <Card.Details.Detail>
                <Card.Details.Detail.Label>
                  {t("scm-review-plugin.pullRequests.details.tasks")}
                </Card.Details.Detail.Label>
                <Card.Details.Detail.Tag>
                  {pullRequest.tasks?.done}/{pullRequest.tasks ? pullRequest.tasks.todo + pullRequest.tasks.done : 0}
                </Card.Details.Detail.Tag>
              </Card.Details.Detail>
              <ReviewersDetail pullRequest={pullRequest} />
              {!error && data?.enabled && data?.rules.length && pullRequest._links.workflowResult ? (
                <PullRequestStatusColumn pullRequest={pullRequest} repository={repository} />
              ) : null}
              <ExtensionPoint<PullRequestListDetailExtension>
                name="pull-requests.list.detail"
                props={{
                  pullRequest,
                  repository
                }}
                renderAll
              />
            </Card.Details>
            <span
              className={classNames(
                "tag is-rounded is-align-self-flex-end",
                `is-${evaluateTagColor(pullRequest.status)}`
              )}
              aria-label={t("scm-review-plugin.pullRequests.aria.status")}
            >
              {pullRequest.status}
            </span>
          </Card.Row>
        </CardListBox.Card>
      ))}
    </CardListBox>
  );
};

export default PullRequestList;
