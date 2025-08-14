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
import { ErrorNotification, Label, Select } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { PullRequest } from "./types/PullRequest";
import { Branch } from "@scm-manager/ui-types";

type Props = {
  pullRequest: PullRequest;
  branches?: Branch[];
  branchesLoading?: boolean;
  branchesError?: Error | null;
  handleFormChange: (target: string) => void;
};

const BranchEditForm: FC<Props> = ({ branches, pullRequest, branchesError, handleFormChange }) => {
  const [t] = useTranslation("plugins");

  if (branchesError) {
    return <ErrorNotification error={branchesError} />;
  }

  const createOptions = () => {
    return branches
      ?.map(branch => ({
        label: branch.name,
        value: branch.name
      }))
      .filter(branch => branch.label !== pullRequest.source);
  };

  return (
    <div className="is-clipped">
      <Label>{t("scm-review-plugin.pullRequest.targetBranch")}</Label>
      <Select
        className=""
        name="target"
        options={createOptions() || []}
        onChange={event => handleFormChange(event.target.value)}
        value={pullRequest?.target}
      />
    </div>
  );
};

export { BranchEditForm };
