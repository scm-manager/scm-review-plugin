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

import React, { FC, useCallback, useEffect, useState } from "react";
import { ErrorNotification, Level, Notification, SubmitButton, Subtitle } from "@scm-manager/ui-components";
import { useDocumentTitleForRepository } from "@scm-manager/ui-core";
import { Checkbox } from "@scm-manager/ui-forms";
import { Branch, Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import { BasicPullRequest, CheckResult, PullRequest } from "./types/PullRequest";
import { useCheckPullRequest, useCreatePullRequest } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import { useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useBranches } from "@scm-manager/ui-api";
import queryString from "query-string";
import usePullRequestTemplate from "./config/usePullRequestTemplate";

type Props = {
  repository: Repository;
};

const Create: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  useDocumentTitleForRepository(repository, t("scm-review-plugin.create.title"));
  const history = useHistory();
  const [pullRequest, setPullRequest] = useState<PullRequest>({
    title: "",
    description: "",
    target: "",
    source: "",
    status: "OPEN",
    labels: [],
    initialTasks: [],
    shouldDeleteSourceBranch: false,
    _links: {}
  });
  const [disabled, setDisabled] = useState(true);
  const location = useLocation();

  const pullRequestCreated = (pullRequestId: string) => {
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequestId}/comments/`);
  };

  const { data: branchesData, error: branchesError, isLoading: branchesLoading } = useBranches(repository);
  const { error: createError, isLoading: createLoading, create } = useCreatePullRequest(repository, pullRequestCreated);
  const { data: checkResult } = useCheckPullRequest(repository, pullRequest, (result: CheckResult) => {
    setDisabled(!isPullRequestValid(pullRequest, result));
  });
  const { data: pullRequestTemplate, isLoading: isLoadingPullRequestTemplate } = usePullRequestTemplate(
    repository,
    pullRequest.source,
    pullRequest.target
  );
  const isValid = useCallback(
    (result?: CheckResult) => {
      if (result) {
        return result?.status === "PR_VALID";
      }
      return checkResult?.status === "PR_VALID";
    },
    [checkResult]
  );
  const isPullRequestValid = useCallback(
    (basicPR: BasicPullRequest, result?: CheckResult) =>
      !!basicPR.source && !!basicPR.target && !!basicPR.title && isValid(result),
    [isValid]
  );
  const handleFormChange = useCallback((basicPR: Partial<PullRequest>) => {
    setPullRequest(prevPr => ({ ...prevPr, ...basicPR }));
  }, []);

  useEffect(() => {
    setDisabled(!isPullRequestValid(pullRequest));
  }, [isPullRequestValid, pullRequest]);

  useEffect(() => {
    if (branchesData) {
      const branches = branchesData?._embedded?.branches;
      const url = location.search;
      const params = queryString.parse(url);
      const branchNames = branches?.map((b: Branch) => b.name);
      const defaultBranch = branches?.find((b: Branch) => b.defaultBranch);

      const initialSource = params.source || (branchNames && branchNames[0]);
      const initialTarget = params.target || defaultBranch?.name;
      const initialStatus = "OPEN";

      handleFormChange({
        source: initialSource,
        target: initialTarget,
        status: initialStatus
      });
    }
  }, [branchesData]);

  useEffect(() => {
    if (pullRequestTemplate) {
      handleFormChange({
        title: pullRequestTemplate.title ?? "",
        description: pullRequestTemplate.description,
        reviewer: pullRequestTemplate?.defaultReviewers.map(it => ({ ...it, approved: false })),
        initialTasks: pullRequestTemplate.defaultTasks,
        shouldDeleteSourceBranch: pullRequestTemplate.shouldDeleteSourceBranch
      });
    }
  }, [pullRequestTemplate]);

  const submit = () => create(pullRequest);

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
        mergePreventReasons={[]}
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
            branches={branchesData?._embedded?.branches}
            handleFormChange={handleFormChange}
            checkResult={checkResult}
            branchesLoading={branchesLoading}
            branchesError={branchesError}
            disabled={isLoadingPullRequestTemplate}
            availableLabels={pullRequestTemplate?.availableLabels ?? []}
            shouldDeleteSourceBranch={pullRequestTemplate?.shouldDeleteSourceBranch ?? false}
          />
        )}
        {information}
        <Level
          right={
            <div className="is-flex is-flex-direction-column is-align-items-end">
              <Checkbox
                checked={pullRequest?.status === "DRAFT"}
                onChange={event => handleFormChange({ status: event.target.checked ? "DRAFT" : "OPEN" })}
                label={t("scm-review-plugin.pullRequest.createAsDraft")}
              />
              <SubmitButton
                label={t("scm-review-plugin.create.submitButton")}
                action={submit}
                loading={createLoading}
                disabled={disabled || isLoadingPullRequestTemplate}
              />
            </div>
          }
        />
      </div>
    </div>
  );
};

export default Create;
