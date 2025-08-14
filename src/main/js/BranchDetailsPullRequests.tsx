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
import { AvatarImage, DateFromNow, SmallLoadingSpinner } from "@scm-manager/ui-components";
import { Popover } from "@scm-manager/ui-overlays";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { PullRequest } from "./types/PullRequest";
import { extensionPoints } from "@scm-manager/ui-extensions";
import { Card } from "@scm-manager/ui-layout";

const PullRequestEntry = styled.div`
  border-radius: 5px;
`;

const UnbreakableText = styled.span`
  word-break: keep-all;
`;

type PRListProps = {
  repository: Repository;
  pullRequests: PullRequest[];
};

const PullRequestList: FC<PRListProps> = ({ repository, pullRequests }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();

  return (
    <>
      {pullRequests.map((pr, key) => {
        if (key > 2) {
          return null;
        }
        return (
          <>
            <PullRequestEntry
              key={pr.id}
              onClick={() => history.push(`/repo/${repository.namespace}/${repository.name}/pull-request/${pr.id}`)}
              className="is-clickable p-1 has-hover-background-blue"
            >
              <div className="px-2 pb-2">
                <strong className="is-size-6">{pr.title}</strong>
                <div className="is-flex is-justify-content-space-between is-size-7">
                  <div className="is-flex is-flex-direction-row is-align-items-center is-justify-content-center">
                    <AvatarImage
                      className="image is-16x16 mr-1"
                      person={{ name: pr.author?.displayName || "", mail: pr.author?.mail }}
                      representation="rounded-border"
                    />
                    <span>{pr.author?.displayName || pr.author?.mail || ""}</span>
                  </div>
                  <DateFromNow date={pr.creationDate} className="is-justify-content-flex-end" />
                </div>
                <div className="is-flex is-justify-content-space-between is-size-7">
                  <UnbreakableText className="pr-1">
                    {t("scm-review-plugin.branchDetails.pullRequest.targetBranch")}
                    {": "}
                  </UnbreakableText>
                  <span className="is-flex-grow-0 is-ellipsis-overflow">{pr.target}</span>
                </div>
              </div>
            </PullRequestEntry>
            {key < pullRequests.length - 1 && key < 2 ? <hr className="m-0" /> : null}
          </>
        );
      })}
    </>
  );
};

const BranchDetailsPullRequests: extensionPoints.BranchListDetail["type"] = ({ repository, branchDetails }) => {
  const [t] = useTranslation("plugins");

  if (!branchDetails) {
    return <SmallLoadingSpinner />;
  }

  const prs: PullRequest[] = (branchDetails._embedded?.pullRequests ?? []) as PullRequest[];

  return (
    <Popover
      trigger={
        <Card.Details.ButtonDetail>
          <Card.Details.Detail.Label>
            {t("scm-review-plugin.pullRequests.details.pullRequests")}
          </Card.Details.Detail.Label>
          <Card.Details.Detail.Tag>{prs.length}</Card.Details.Detail.Tag>
        </Card.Details.ButtonDetail>
      }
      title={
        <h1 className="has-text-weight-bold is-size-5">{t("scm-review-plugin.branchDetails.pullRequest.popover")}</h1>
      }
    >
      <PullRequestList repository={repository} pullRequests={prs} />
    </Popover>
  );
};

export default BranchDetailsPullRequests;
