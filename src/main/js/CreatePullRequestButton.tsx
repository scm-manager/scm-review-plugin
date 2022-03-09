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
import { useTranslation } from "react-i18next";
import { Branch, Repository } from "@scm-manager/ui-types";
import { SubSubtitle, AddButton, ErrorNotification, Loading } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import PullRequestTable from "./table/PullRequestTable";
import styled from "styled-components";
import { usePullRequests } from "./pullRequest";

type Props = {
  repository: Repository;
  branch: Branch;
};

const HR = styled.hr`
  height: 3px;
`;

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

const CreatePullRequestButton: FC<Props> = ({ repository, branch }) => {
  const [t] = useTranslation("plugins");
  const { data, error, isLoading } = usePullRequests(repository);

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
    (pr: PullRequest) => pr.source === branch.name
  );

  let existing = null;
  if (matchingPullRequests.length > 0) {
    existing = (
      <ScrollingTable className="mb-3">
        <PullRequestTable repository={repository} pullRequests={matchingPullRequests} />
      </ScrollingTable>
    );
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
