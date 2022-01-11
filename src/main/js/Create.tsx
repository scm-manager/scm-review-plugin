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
import React, { FC, useEffect, useState } from "react";
import { ErrorNotification, Level, Notification, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Branch, Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import styled from "styled-components";
import { BasicPullRequest, CheckResult, PullRequest } from "./types/PullRequest";
import { useCheckPullRequest, useCreatePullRequest } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import { useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useBranches } from "@scm-manager/ui-api";
import queryString from "query-string";

type Props = {
  repository: Repository;
};

const Create: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const [pullRequest, setPullRequest] = useState<PullRequest>({ title: "", target: "", source: "", _links: {} });
  const [disabled, setDisabled] = useState(true);
  const location = useLocation();

  const pullRequestCreated = (pullRequestId: string) => {
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequestId}/comments`);
  };

  const { data: branchesData, error: branchesError, isLoading: branchesLoading } = useBranches(repository);
  const { error: createError, isLoading: createLoading, create } = useCreatePullRequest(repository, pullRequestCreated);
  const { data: checkResult } = useCheckPullRequest(repository, pullRequest, (result: CheckResult) => {
    setDisabled(!isPullRequestValid(pullRequest, result));
  });

  const branches = branchesData?._embedded.branches;

  useEffect(() => {
    if (branchesData) {
      const url = location.search;
      const params = queryString.parse(url);
      const branchNames = branches?.map((b: Branch) => b.name);
      const defaultBranch = branches?.find((b: Branch) => b.defaultBranch);

      const initialSource = params.source || (branchNames && branchNames[0]);
      const initialTarget = params.target || defaultBranch?.name;

      handleFormChange({
        title: "",
        source: initialSource,
        target: initialTarget,
        _links: {}
      });
    }
  }, [branchesData]);

  const submit = () => create(pullRequest);

  const isValid = (result?: CheckResult) => {
    if (result) {
      return result?.status === "PR_VALID";
    }
    return checkResult?.status === "PR_VALID";
  };

  const isPullRequestValid = (basicPR: BasicPullRequest, result?: CheckResult) => {
    return !!basicPR.source && !!basicPR.target && !!basicPR.title && isValid(result);
  };

  const handleFormChange = (basicPR: PullRequest) => {
    setPullRequest(basicPR);
    setDisabled(!isPullRequestValid(basicPR));
  };

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  let notification = null;
  if (createError) {
    notification = <ErrorNotification error={createError} />;
  }

  let information = null;
  if (!createLoading && pullRequest?.source && pullRequest?.target) {
    information = (
      <PullRequestInformation
        repository={repository}
        pullRequest={pullRequest}
        status="OPEN"
        mergeHasNoConflict={true}
        shouldFetchChangesets={isValid()}
        source={pullRequest.source}
        target={pullRequest.target}
        targetBranchDeleted={false}
      />
    );
  }
  return (
    <div className="columns">
      <div className="column is-clipped">
        <Subtitle subtitle={t("scm-review-plugin.create.subtitle", { repositoryName: repository.name })} />
        {notification}
        {!createLoading && (
          <CreateForm
            pullRequest={pullRequest}
            branches={branchesData?._embedded.branches}
            handleFormChange={handleFormChange}
            checkResult={checkResult}
            branchesLoading={branchesLoading}
            branchesError={branchesError}
          />
        )}
        {information}
        <Level
          className="pt-5"
          right={
            <SubmitButton
              label={t("scm-review-plugin.create.submitButton")}
              action={submit}
              loading={createLoading}
              disabled={disabled}
            />
          }
        />
      </div>
    </div>
  );
};

export default Create;
