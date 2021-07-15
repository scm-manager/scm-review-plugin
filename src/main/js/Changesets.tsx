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
import React, { FC, useEffect, useState } from "react";
import { useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Repository } from "@scm-manager/ui-types";
import { usePullRequestChangesets } from "./pullRequest";
import {
  ChangesetList,
  ErrorNotification,
  LinkPaginator,
  Loading,
  NotFoundError,
  Notification
} from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  shouldFetchChangesets?: boolean;
};

const Changesets: FC<Props> = ({ repository, pullRequest, shouldFetchChangesets = true }) => {
  const [t] = useTranslation("plugins");
  const match = useRouteMatch<{ page: string }>();
  const [page, setPage] = useState(1);

  const { data: changesets, error, isLoading } = usePullRequestChangesets(repository, pullRequest, page);

  useEffect(() => {
    if (match.params.page) {
      setPage(parseInt(match.params.page, 10));
    }
  }, [match]);

  const renderPaginator = () => {
    if (changesets && changesets.pageTotal > 1) {
      return (
        <div className="panel-footer">
          <LinkPaginator collection={changesets} page={page} />
        </div>
      );
    }
    return null;
  };

  if (error instanceof NotFoundError || !shouldFetchChangesets) {
    return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
  } else if (error) {
    return <ErrorNotification error={error} />;
  } else if (isLoading) {
    return <Loading />;
  } else if (changesets && changesets._embedded && changesets._embedded.changesets) {
    if (changesets._embedded.changesets?.length !== 0) {
      return (
        <div className="panel">
          <div className="panel-block">
            <ChangesetList repository={repository} changesets={changesets._embedded.changesets} />
          </div>
          {renderPaginator()}
        </div>
      );
    } else {
      return <Notification type="info">{t("scm-review-plugin.pullRequest.noChangesets")}</Notification>;
    }
  }
  return null;
};

export default Changesets;
