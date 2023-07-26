/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { PullRequest, Reviewer } from "./types/PullRequest";
import { CardList } from "@scm-manager/ui-layout";
import { Link } from "react-router-dom";
import { DateFromNow, Notification, useGeneratedId } from "@scm-manager/ui-components";
import { PullRequestListDetail } from "./types/ExtensionPoints";
import classNames from "classnames";
import { evaluateTagColor } from "./pullRequest";
import React, { FC, useMemo } from "react";
import { Repository } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useBinder } from "@scm-manager/ui-extensions";
import { Trans, useTranslation } from "react-i18next";
import PullRequestStatusColumn from "./workflow/PullRequestStatusColumn";
import useEngineConfig from "./workflow/useEngineConfig";
import { Tooltip } from "@scm-manager/ui-overlays";

const PullRequestDetail: FC<{
  pullRequest: PullRequest;
  repository: Repository;
  detail: PullRequestListDetail["type"];
}> = ({ repository, detail, pullRequest }) => {
  const labelId = useGeneratedId();
  const renderedDetail = detail.render({ pullRequest, repository, labelId });
  if (!renderedDetail) {
    return null;
  }
  return (
    <span key={detail.name}>
      <span className="has-text-secondary mr-1" id={labelId}>
        {detail.name}
      </span>
      {renderedDetail}
    </span>
  );
};

const DetailsContainer = styled.span`
  gap: 0.5rem 1rem;
`;

const PullRequestCard = styled(CardList.Card)`
  & > div {
    gap: 0.5rem;
  }
`;

const SubtitleContainer = styled(CardList.Card.Row)`
  white-space: pre;
`;

const createTooltipMessage = (reviewers: Reviewer[]) => {
  return reviewers.map(({ displayName, approved }) => `- ${displayName}${approved ? " ✔" : ""}`).join("\n");
};

type Props = {
  repository: Repository;
  pullRequests?: PullRequest[];
};

const PullRequestList: FC<Props> = ({ pullRequests, repository }) => {
  const binder = useBinder();
  const [t] = useTranslation("plugins");
  const { data, error } = useEngineConfig(repository);
  const defaultPullRequestListDetails = useMemo(() => {
    const result: PullRequestListDetail["type"][] = [
      {
        name: t("scm-review-plugin.pullRequests.details.tasks"),
        render: ({ pullRequest, labelId }) => (
          <span className="tag is-rounded is-light" aria-labelledby={labelId}>
            {pullRequest.tasks?.done}/{pullRequest.tasks ? pullRequest.tasks.todo + pullRequest.tasks.done : 0}
          </span>
        )
      },
      {
        name: t("scm-review-plugin.pullRequests.details.reviewers"),
        render: ({ pullRequest, labelId }) => {
          const content = (
            <span
              className={classNames("tag is-rounded is-light", { "is-relative": pullRequest.reviewer?.length })}
              aria-labelledby={labelId}
            >
              {pullRequest.reviewer?.reduce((p, { approved }) => (approved ? p + 1 : p), 0)}/
              {pullRequest.reviewer?.length ?? 0}
            </span>
          );
          if (pullRequest.reviewer?.length) {
            return (
              <Tooltip
                message={
                  t("scm-review-plugin.pullRequest.reviewer") + ":\n" + createTooltipMessage(pullRequest.reviewer)
                }
              >
                {content}
              </Tooltip>
            );
          }
          return content;
        }
      }
    ];
    if (!error && data?.enabled && data?.rules.length) {
      result.push({
        name: t("scm-review-plugin.pullRequests.details.workflow"),
        render: ({ pullRequest }) =>
          pullRequest._links.workflowResult ? (
            <PullRequestStatusColumn pullRequest={pullRequest} repository={repository} />
          ) : null
      });
    }
    return result;
  }, [data, error, repository, t]);

  if (!pullRequests?.length) {
    return <Notification type="info">{t("scm-review-plugin.noRequests")}</Notification>;
  }

  return (
    <CardList>
      {pullRequests.map(pullRequest => (
        <PullRequestCard key={pullRequest.id}>
          <CardList.Card.Row className="is-flex">
            <CardList.Card.Title>
              <Link to={`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`}>
                {pullRequest.title}
              </Link>
            </CardList.Card.Title>
            <span className="has-text-secondary ml-1" aria-label={t("scm-review-plugin.pullRequests.aria.id")}>
              <span aria-hidden>#</span>
              {pullRequest.id}
            </span>
          </CardList.Card.Row>
          <SubtitleContainer className="is-flex is-flex-wrap-wrap is-size-7 has-text-secondary">
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
          </SubtitleContainer>
          <CardList.Card.Row className="is-flex is-align-items-center is-justify-content-space-between is-size-7">
            <DetailsContainer className="is-flex is-flex-wrap-wrap is-align-items-center">
              {[
                ...defaultPullRequestListDetails,
                ...binder.getExtensions<PullRequestListDetail>("pull-requests.list.detail", {
                  pullRequest,
                  repository
                })
              ].map(detail => (
                <PullRequestDetail
                  key={detail.name}
                  pullRequest={pullRequest}
                  detail={detail}
                  repository={repository}
                />
              ))}
            </DetailsContainer>
            <span
              className={classNames(
                "tag is-rounded is-align-self-flex-end",
                `is-${evaluateTagColor(pullRequest.status)}`
              )}
              aria-label={t("scm-review-plugin.pullRequests.aria.status")}
            >
              {pullRequest.status}
            </span>
          </CardList.Card.Row>
        </PullRequestCard>
      ))}
    </CardList>
  );
};

export default PullRequestList;
