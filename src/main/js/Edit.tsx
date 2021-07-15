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
import { ErrorNotification, Level, SubmitButton, Subtitle, Title, Loading } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import EditForm from "./EditForm";
import { useTranslation } from "react-i18next";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useUpdatePullRequest } from "./pullRequest";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const Edit: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch();
  const history = useHistory();
  const [modifiedPullRequest, setModifiedPullRequest] = useState<PullRequest>(pullRequest);

  const pullRequestUpdated = () => {
    history.push({
      pathname: `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments`,
      state: {
        from: match.url + "/updated"
      }
    });
  };

  const { error, isLoading, update } = useUpdatePullRequest(repository, pullRequest, pullRequestUpdated);

  const submit = () => {
    update(modifiedPullRequest);
  };

  const handleFormChange = (pr: PullRequest) => {
    setModifiedPullRequest(pr);
  };

  if (isLoading) {
    return <Loading />;
  }

  return (
    <div className="columns">
      <div className="column">
        <Title title={t("scm-review-plugin.edit.title")} />
        <Subtitle subtitle={t("scm-review-plugin.edit.subtitle", { repositoryName: repository.name })} />
        <ErrorNotification error={error} />
        <EditForm pullRequest={modifiedPullRequest} handleFormChange={handleFormChange} />
        <Level
          right={<SubmitButton label={t("scm-review-plugin.edit.submitButton")} action={submit} loading={isLoading} disabled={!modifiedPullRequest.title} />}
        />
      </div>
    </div>
  );
};

export default Edit;
