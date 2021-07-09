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
import { ErrorNotification, Loading, Notification } from "@scm-manager/ui-components";
import { Route, Switch, useRouteMatch } from "react-router-dom";
import PullRequestDetails from "./PullRequestDetails";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequest } from "./pullRequest";
import Edit from "./Edit";
import { useTranslation } from "react-i18next";

type Props = {
  repository: Repository;
};

const SinglePullRequest: FC<Props> = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch<{ pullRequestNumber: string }>();
  const { data, error, isLoading } = usePullRequest(repository, match?.params?.pullRequestNumber);

  if (!repository._links.pullRequest) {
    return <Notification type="danger">{t("scm-review-plugin.pullRequests.forbidden")}</Notification>;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!data || isLoading) {
    return <Loading />;
  }

  return (
    <Switch>
      <Route path={`${match.url}/edit`} exact>
        <Edit repository={repository} pullRequest={data} />
      </Route>
      <Route path={`${match.url}`}>
        <PullRequestDetails repository={repository} pullRequest={data} />
      </Route>
    </Switch>
  );
};

export default SinglePullRequest;
