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
