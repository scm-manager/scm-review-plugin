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
import { Notification, Button } from "@scm-manager/ui-components";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";

type PullRequestFileProps = {
  oldPath: string;
  newPath: string;
};

type PartialNotificationProps = {
  pullRequestComments: Comment[];
  pullRequestFiles?: PullRequestFileProps[];
  fetchNextPage: () => void;
  isFetchingNextPage: boolean;
};

const PartialNotification: FC<PartialNotificationProps> = ({
  pullRequestComments,
  pullRequestFiles,
  fetchNextPage,
  isFetchingNextPage
}) => {
  const [t] = useTranslation("plugins");

  const filteredPullRequestComments = pullRequestComments.filter(comment => !comment.outdated && !!comment.location);
  const totalCommentCount = filteredPullRequestComments?.length || 0;
  const partialFiles = pullRequestFiles?.map(file => (file.newPath !== "/dev/null" ? file.newPath : file.oldPath));
  const partialCommentCount =
    filteredPullRequestComments?.filter(comment => !!comment.location && partialFiles?.includes(comment.location.file))
      .length || 0;
  const notificationType = partialCommentCount < totalCommentCount ? "warning" : "info";

  return (
    <Notification className="mt-5" type={notificationType}>
      <div className="columns is-centered is-align-items-center">
        <div className="column">
          {partialCommentCount < totalCommentCount
            ? t("scm-review-plugin.diff.partialMoreDiffsAvailable", {
                count: totalCommentCount - partialCommentCount
              })
            : t("scm-review-plugin.diff.moreDiffsAvailable")}
        </div>
        <Button label={t("scm-review-plugin.diff.loadMore")} action={fetchNextPage} loading={isFetchingNextPage} />
      </div>
    </Notification>
  );
};

export default PartialNotification;
