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
