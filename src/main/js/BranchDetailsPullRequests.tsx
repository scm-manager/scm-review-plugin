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

import React, { FC } from "react";
import { Branch, BranchDetails, Repository } from "@scm-manager/ui-types";
import {
  Icon,
  SmallLoadingSpinner,
  usePopover,
  Popover,
  CardColumnSmall,
  DateFromNow,
  AvatarImage
} from "@scm-manager/ui-components";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { PullRequest } from "./types/PullRequest";

const StyledText = styled.span.attrs(() => ({
  className: "has-text-grey is-size-7"
}))``;

const UnbreakableText = styled.span`
  word-break: keep-all;
`;

type Props = {
  repository: Repository;
  branch: Branch;
  details?: BranchDetails;
};

type PRListProps = {
  repository: Repository;
  pullRequests: PullRequest[];
};

const PullRequestList: FC<PRListProps> = ({ repository, pullRequests }) => {
  const [t] = useTranslation("plugins");

  return (
    <>
      {pullRequests.map((pr, key) => {
        if (key > 2) {
          return null;
        }
        return (
          <>
            <CardColumnSmall
              key={key}
              link={`/repo/${repository.namespace}/${repository.name}/pull-request/${pr.id}`}
              contentLeft={
                <div className="has-text-grey pb-2">
                  <strong>{pr.title}</strong>
                  <div className="is-flex is-justify-content-space-between">
                    <div className="is-flex is-flex-direction-row is-align-items-center is-justify-content-center">
                      <AvatarImage
                        className="image is-16x16 mr-1"
                        person={{ name: pr.author?.displayName || "", mail: pr.author?.mail }}
                        representation="rounded-border"
                      />
                      <span className="has-size-7">{pr.author?.displayName || pr.author?.mail || ""}</span>
                    </div>
                    <DateFromNow date={pr.creationDate} className="is-justify-content-flex-end" />
                  </div>
                </div>
              }
              contentRight={
                <div className="has-text-grey is-flex is-justify-content-space-between">
                  <UnbreakableText className="pr-1">
                    {t("scm-review-plugin.branchDetails.pullRequest.targetBranch")}
                    {": "}
                  </UnbreakableText>
                  <span className="is-flex-grow-0 is-ellipsis-overflow">{pr.target}</span>
                </div>
              }
            />
            {key < pullRequests.length - 1 && key < 2 ? <hr className="m-0" /> : null}
          </>
        );
      })}
    </>
  );
};

const BranchDetailsPullRequests: FC<Props> = ({ repository, branch, details }) => {
  const history = useHistory();
  const { popoverProps, triggerProps } = usePopover();
  const [t] = useTranslation("plugins");

  if (branch.defaultBranch) {
    return null;
  }

  if (!details) {
    return <SmallLoadingSpinner />;
  }

  const prs: PullRequest[] = (details._embedded?.pullRequests as PullRequest[]) || [];

  if (prs.length > 0) {
    return (
      <div className="is-relative">
        <Popover
          title={
            <h1 className="has-text-weight-bold is-size-5">
              {t("scm-review-plugin.branchDetails.pullRequest.popover")}
            </h1>
          }
          width={400}
          {...popoverProps}
        >
          <PullRequestList repository={repository} pullRequests={prs} />
        </Popover>
        <div {...triggerProps}>
          <Icon name="code-branch" className="is-size-7" />
          <StyledText>
            {t("scm-review-plugin.branchDetails.pullRequest.pending", {
              count: prs.length
            })}
          </StyledText>
        </div>
      </div>
    );
  }

  return (
    <div
      className="is-clickable"
      onClick={() =>
        history.push(
          `/repo/${repository.namespace}/${repository.name}/pull-requests/add/changesets/?source=${encodeURI(
            branch.name
          )}`
        )
      }
    >
      <Icon name="code-branch" className="is-size-7" />
      <StyledText>{t("scm-review-plugin.branchDetails.pullRequest.create")}</StyledText>
    </div>
  );
};

export default BranchDetailsPullRequests;
