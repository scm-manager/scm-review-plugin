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
import { PullRequest } from "../types/PullRequest";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequestChanges } from "../pullRequest";
import { ErrorNotification, Loading } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import ChangeContainer from "./ChangeContainer";

type Props = {
  pullRequest: PullRequest;
  repository: Repository;
};

const RootChangesContainer: FC<Props> = ({ pullRequest, repository }) => {
  const { t } = useTranslation("plugins");
  const { error, isLoading, data } = usePullRequestChanges(repository, pullRequest);

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (data?.length === 0) {
    return <p>{t("scm-review-plugin.pullRequest.changes.noChanges")}</p>;
  }

  return (
    <div>
      <ul>
        {data
          ? data.map(change => (
              <li>
                <ChangeContainer change={change} repository={repository} />
              </li>
            ))
          : null}
      </ul>
    </div>
  );
};

export default RootChangesContainer;
