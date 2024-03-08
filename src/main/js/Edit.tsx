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
import React, { FC, useCallback, useState } from "react";
import { SubmitButton } from "@scm-manager/ui-components";
import { Checkbox, ErrorNotification, Label, Level, Loading, Subtitle } from "@scm-manager/ui-core";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import EditForm from "./EditForm";
import { useTranslation } from "react-i18next";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useUpdatePullRequest } from "./pullRequest";
import { useBranches } from "@scm-manager/ui-api";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const Edit: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch();
  const history = useHistory();
  const [modifiedPullRequest, setModifiedPullRequest] = useState<PullRequest>(pullRequest);
  const { data: branchesData, error: branchesError, isLoading: branchesLoading } = useBranches(repository);

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

  return (
    <div className="columns">
      <div className="column">
        <Subtitle subtitle={t("scm-review-plugin.edit.subtitle", { repositoryName: repository.name })} />
        <ErrorNotification error={error} />
        <EditForm
          pullRequest={modifiedPullRequest}
          handleFormChange={handleFormChange}
          availableLabels={pullRequest?._embedded?.availableLabels.availableLabels ?? []}
          entrypoint="edit"
          shouldDeleteSourceBranch={pullRequest.shouldDeleteSourceBranch}
          branches={branchesData?._embedded?.branches}
          branchesLoading={branchesLoading}
          branchesError={branchesError}
        />
        {pullRequest.status === "OPEN" ? (
          <div>
            <Label>{t("scm-review-plugin.pullRequest.status")}</Label>
            <Checkbox
              checked={modifiedPullRequest?.status === "DRAFT"}
              onChange={value => handleFormChange({ status: value ? "DRAFT" : "OPEN" })}
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
              disabled={!modifiedPullRequest.title}
            />
          }
        />
      </div>
    </div>
  );
};

export default Edit;
