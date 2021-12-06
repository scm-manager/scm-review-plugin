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
import { Branch, BranchDetails, HalRepresentation, Repository } from "@scm-manager/ui-types";
import { Icon, SmallLoadingSpinner, usePopover, Popover } from "@scm-manager/ui-components";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { PullRequest } from "./types/PullRequest";

const StyledText = styled.span.attrs(() => ({
  className: "has-text-grey is-size-7"
}))``;

type Props = {
  repository: Repository;
  branch: Branch;
  details?: BranchDetails;
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

  const prs: PullRequest[] =
    ((details._embedded?.pullRequests as HalRepresentation)?._embedded?.pullRequests as PullRequest[]) || [];

  if (prs.length > 0) {
    return (
      <div className="is-relative">
        <Popover
          title={
            <h1 className="has-text-weight-bold is-size-5">
              {t("scm-review-plugin.branchDetails.pullRequest.popover")}
            </h1>
          }
          width={500}
          {...popoverProps}
        >
          abc
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
