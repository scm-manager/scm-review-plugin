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

import React, { FC, useCallback, useState } from "react";
import { SubmitButton } from "@scm-manager/ui-components";
import { Checkbox, ErrorNotification, Label, Level, Loading, Subtitle } from "@scm-manager/ui-core";
import { Repository } from "@scm-manager/ui-types";
import { CheckResult, PullRequest } from "./types/PullRequest";
import EditForm from "./EditForm";
import { useTranslation } from "react-i18next";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useCheckPullRequest, useUpdatePullRequest } from "./pullRequest";
import { useBranches } from "@scm-manager/ui-api";
import { CheckResultDisplay } from "./CheckResultDisplay";
import { BranchEditForm } from "./BranchEditForm";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const Edit: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch();
  const history = useHistory();
  const [modifiedPullRequest, setModifiedPullRequest] = useState<PullRequest>(pullRequest);
  const [targetBranchValid, setTargetBranchValid] = useState(true);
  const { data: branchesData, error: branchesError, isLoading: branchesLoading } = useBranches(repository);
  const { data: checkResult } = useCheckPullRequest(repository, modifiedPullRequest, (result: CheckResult) => {
    setTargetBranchValid(result?.status === "PR_VALID");
  });

  const pullRequestUpdated = useCallback(() => {
    history.push({
      pathname: `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`,
      state: {
        from: match.url + "/updated"
      }
    });
  }, [history, match, pullRequest, repository]);

  const { error, isLoading, update } = useUpdatePullRequest(repository, pullRequest, pullRequestUpdated);

  const handleFormChange = useCallback((pr: Partial<PullRequest>) => {
    setModifiedPullRequest(prev => ({ ...prev, ...pr }));
  }, []);

  const submit = () => {
    update(modifiedPullRequest);
  };

  if (isLoading) {
    return <Loading />;
  }

  const changesValid = () => targetBranchValid && modifiedPullRequest.title;

  return (
    <div className="columns">
      <div className="column is-clipped">
        <Subtitle subtitle={t("scm-review-plugin.edit.subtitle", { repositoryName: repository.name })} />
        <ErrorNotification error={error} />
        <BranchEditForm
          pullRequest={modifiedPullRequest}
          branches={branchesData?._embedded?.branches}
          branchesLoading={branchesLoading}
          branchesError={branchesError}
          handleFormChange={target => handleFormChange({ target })}
        />
        <CheckResultDisplay checkResult={checkResult} />
        <EditForm
          pullRequest={modifiedPullRequest}
          handleFormChange={handleFormChange}
          availableLabels={pullRequest?._embedded?.availableLabels.availableLabels ?? []}
          shouldDeleteSourceBranch={pullRequest.shouldDeleteSourceBranch}
        />
        {pullRequest.status === "OPEN" ? (
          <div>
            <Label>{t("scm-review-plugin.pullRequest.status")}</Label>
            <Checkbox
              checked={modifiedPullRequest?.status === "DRAFT"}
              onChange={event => handleFormChange({ status: event.target.checked ? "DRAFT" : "OPEN" })}
              label={t("scm-review-plugin.pullRequest.changeToDraft")}
              helpText={t("scm-review-plugin.pullRequest.changeToDraftHelpText")}
              disabled={modifiedPullRequest.status === "MERGED" || modifiedPullRequest.status === "REJECTED"}
            />
          </div>
        ) : null}
        <Level
          right={
            <SubmitButton
              label={t("scm-review-plugin.edit.submitButton")}
              action={submit}
              loading={isLoading}
              disabled={!changesValid()}
            />
          }
        />
      </div>
    </div>
  );
};

export default Edit;
