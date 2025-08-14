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
import { Branch } from "@scm-manager/ui-types";
import { ErrorNotification, Select } from "@scm-manager/ui-components";
import { CheckResult, PullRequest } from "./types/PullRequest";
import { useTranslation } from "react-i18next";
import EditForm from "./EditForm";
import styled from "styled-components";
import InitialTasks from "./InitialTasks";
import { CheckResultDisplay } from "./CheckResultDisplay";

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

const autoFocus = (el: HTMLSelectElement) => el?.focus();

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
            onChange={e => handleFormChange({ source: e.target.value })}
            loading={branchesLoading}
            value={pullRequest?.source}
            ref={autoFocus}
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
        <CheckResultDisplay checkResult={checkResult} />
        <EditForm
          handleFormChange={handleFormChange}
          pullRequest={pullRequest}
          disabled={disabled}
          availableLabels={availableLabels}
          shouldDeleteSourceBranch={shouldDeleteSourceBranch}
        />
      </MergeContainer>
      <InitialTasks value={pullRequest.initialTasks} onChange={value => handleFormChange({ initialTasks: value })} />
    </form>
  );
};

export default CreateForm;
