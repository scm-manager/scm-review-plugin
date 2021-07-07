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
import React, { FC, useState } from "react";
import { ErrorNotification, Level, Notification, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Link, Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import styled from "styled-components";
import { BasicPullRequest, CheckResult, PullRequest } from "./types/PullRequest";
import { checkPullRequest, createChangesetUrl, getChangesets, useCreatePullRequest } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";

const TopPaddingLevel = styled(Level)`
  padding-top: 1.5em;
`;

type Props = {
  repository: Repository;
};

const Create: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  const [pullRequest, setPullRequest] = useState<BasicPullRequest>({ title: "", target: "", source: "" });
  const [changesets, setChangesets] = useState([]);
  const [disabled, setDisabled] = useState(true);
  const [checkResult, setCheckResult] = useState<CheckResult | undefined>();

  const pullRequestCreated = (pullRequestId: string) => {
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequestId}/comments`);
  };

  const { error, isLoading, create } = useCreatePullRequest(repository, pullRequestCreated);

  const fetchChangesets = (basicPR: BasicPullRequest) => {
    return checkPullRequest((repository._links.pullRequestCheck as Link)?.href, basicPR)
      .then(r => r.json())
      .then(checkResult => {
        setCheckResult(checkResult);
        if (checkResult?.status === "PR_VALID") {
          getChangesets(createChangesetUrl(repository, basicPR.source, basicPR.target)!).then(result => {
            setPullRequest(basicPR);
            setChangesets(result._embedded.changesets);
          });
        }
      });
  };

  const submit = () => create(pullRequest);

  const isValid = () => {
    return checkResult?.status === "PR_VALID";
  };

  const verify = (basicPR: BasicPullRequest) => {
    const { source, target, title } = basicPR;
    if (source && target && title) {
      return isValid();
    }
    return false;
  };

  const shouldFetchChangesets = (basicPR: BasicPullRequest) => {
    return pullRequest?.source !== basicPR.source || pullRequest.target !== basicPR.target;
  };

  const handleFormChange = (basicPR: BasicPullRequest) => {
    if (shouldFetchChangesets(basicPR)) {
      setPullRequest(basicPR);
      fetchChangesets(basicPR).then(() => {
        setPullRequest(pullRequest);
        setDisabled(!verify(basicPR));
      });
    } else {
      setPullRequest(basicPR);
      setDisabled(!verify(basicPR));
    }
  };

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  let notification = null;
  if (error) {
    notification = <ErrorNotification error={error} />;
  }

  let information = null;
  if (!isLoading && pullRequest?.source && pullRequest?.target) {
    information = (
      <PullRequestInformation
        repository={repository}
        pullRequest={pullRequest as PullRequest}
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
        <Title title={t("scm-review-plugin.create.title")} />
        <Subtitle subtitle={t("scm-review-plugin.create.subtitle", { repositoryName: repository.name })} />
        {notification}
        {!isLoading && (
          <CreateForm
            repository={repository}
            pullRequest={pullRequest}
            onChange={handleFormChange}
            checkResult={checkResult}
          />
        )}
        {information}
        <TopPaddingLevel
          right={
            <SubmitButton
              label={t("scm-review-plugin.create.submitButton")}
              action={submit}
              loading={isLoading}
              disabled={disabled}
            />
          }
        />
      </div>
    </div>
  );
};

export default Create;
