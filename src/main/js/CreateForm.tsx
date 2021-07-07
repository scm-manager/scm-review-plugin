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
import React, { FC, useEffect } from "react";
import {Branch, Repository} from "@scm-manager/ui-types";
import { ErrorNotification, Select } from "@scm-manager/ui-components";
import { useBranches } from "@scm-manager/ui-api";
import { BasicPullRequest, CheckResult, PullRequest } from "./types/PullRequest";
import { useTranslation } from "react-i18next";
import EditForm from "./EditForm";
import styled from "styled-components";
import {useLocation} from "react-router-dom";
import queryString from "query-string";

const ValidationError = styled.p`
  font-size: 0.75rem;
  color: #ff3860;
  margin-top: -3em;
  margin-bottom: 3em;
`;

type Props = {
  repository: Repository;
  pullRequest: BasicPullRequest;
  onChange: (pr: BasicPullRequest) => void;
  checkResult?: CheckResult;
};

const CreateForm: FC<Props> = ({ repository, pullRequest, onChange, checkResult }) => {
  const [t] = useTranslation("plugins");
  const location = useLocation();
  const { data: branchesData, error, isLoading } = useBranches(repository);

  useEffect(() => {
    const url = location.search;
    const params = queryString.parse(url);
    const branchNames = branchesData?._embedded?.branches.map((b: Branch) => b.name);
    const defaultBranch = branchesData?._embedded?.branches.find((b: Branch) => b.defaultBranch);

    const initialSource = params.source || (branchNames && branchNames[0]);
    const initialTarget = params.target || defaultBranch?.name;

    onChange({
      title: "",
      source: initialSource,
      target: initialTarget
    });
  }, [repository, branchesData]);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
  };

  const renderValidationError = () => {
    if (checkResult && checkResult.status !== "PR_VALID") {
      return <ValidationError>{t(`scm-review-plugin.pullRequest.validation.${checkResult.status}`)}</ValidationError>;
    }
  };

  const createOptions = () => {
    return branchesData?._embedded?.branches.map(branch => ({
      label: branch.name,
      value: branch.name
    }));
  };

  if (error) {
    return <ErrorNotification error={error} />;
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="columns">
        <div className="column is-clipped">
          <Select
            name="source"
            label={t("scm-review-plugin.pullRequest.sourceBranch")}
            options={createOptions() || []}
            onChange={value => onChange({ ...pullRequest, source: value })}
            loading={isLoading}
            value={pullRequest?.source}
          />
        </div>
        <div className="column is-clipped">
          <Select
            name="target"
            label={t("scm-review-plugin.pullRequest.targetBranch")}
            options={createOptions() || []}
            onChange={value => onChange({ ...pullRequest, target: value })}
            loading={isLoading}
            value={pullRequest?.target}
          />
        </div>
      </div>
      {renderValidationError()}
      <EditForm handleFormChange={onChange} pullRequest={pullRequest as PullRequest} />
    </form>
  );
};

export default CreateForm;
