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
import { Branch } from "@scm-manager/ui-types";
import { ErrorNotification, Select } from "@scm-manager/ui-components";
import { CheckResult, PullRequest } from "./types/PullRequest";
import { useTranslation } from "react-i18next";
import EditForm from "./EditForm";
import styled from "styled-components";
import InitialTasks from "./InitialTasks";

const ValidationError = styled.fieldset`
  font-size: 0.75rem;
`;

const MergeContainer = styled.fieldset`
  margin-top: -2em;
`;

type Props = {
  pullRequest: PullRequest;
  handleFormChange: (pr: Partial<PullRequest>) => void;
  checkResult?: CheckResult;
  branches?: Branch[];
  branchesError?: Error | null;
  branchesLoading: boolean;
  disabled?: boolean;
  availableLabels: string[];
  shouldDeleteSourceBranch: boolean;
};

const CreateForm: FC<Props> = ({
  pullRequest,
  handleFormChange,
  checkResult,
  branches,
  branchesError,
  branchesLoading,
  disabled,
  availableLabels,
  shouldDeleteSourceBranch
}) => {
  const [t] = useTranslation("plugins");
  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
  };

  const renderValidationError = () => {
    if (checkResult && checkResult.status !== "PR_VALID") {
      return (
        <ValidationError className="has-text-danger">
          {t(`scm-review-plugin.pullRequest.validation.${checkResult.status}`)}
        </ValidationError>
      );
    }
  };

  const createOptions = () => {
    return branches?.map(branch => ({
      label: branch.name,
      value: branch.name
    }));
  };

  if (branchesError) {
    return <ErrorNotification error={branchesError} />;
  }
  return (
    <form onSubmit={handleSubmit}>
      <div className="columns">
        <div className="column">
          <Select
            className="is-fullwidth"
            name="source"
            label={t("scm-review-plugin.pullRequest.sourceBranch")}
            options={createOptions() || []}
            onChange={value => handleFormChange({ source: value })}
            loading={branchesLoading}
            value={pullRequest?.source}
          />
        </div>
        <div className="column is-clipped">
          <Select
            className="is-fullwidth"
            name="target"
            label={t("scm-review-plugin.pullRequest.targetBranch")}
            options={createOptions() || []}
            onChange={value => handleFormChange({ target: value })}
            loading={branchesLoading}
            value={pullRequest?.target}
          />
        </div>
      </div>
      <MergeContainer>
        {renderValidationError()}
        <EditForm
          handleFormChange={handleFormChange}
          pullRequest={pullRequest}
          disabled={disabled}
          availableLabels={availableLabels}
          shouldDeleteSourceBranch={shouldDeleteSourceBranch}
          entrypoint={"create"}
        />
      </MergeContainer>
      <InitialTasks value={pullRequest.initialTasks} onChange={value => handleFormChange({ initialTasks: value })} />
    </form>
  );
};

export default CreateForm;
