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
import { MergePreventReason, PullRequest } from "../types/PullRequest";
import { Branch, Link, Repository } from "@scm-manager/ui-types";
import { File, FileContentFactory, JumpToFileButton, getDiffAnchorId } from "@scm-manager/ui-components";
import { ErrorNotification, Notification, Loading } from "@scm-manager/ui-core";
import { createDiffUrl, useComments } from "../pullRequest";
import { useTranslation } from "react-i18next";
import Diff from "./Diff";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
  sourceBranch?: Branch;
  stickyHeaderHeight: number;
  mergePreventReasons: MergePreventReason[];
};

const DiffRoute: FC<Props> = ({
  repository,
  pullRequest,
  source,
  target,
  sourceBranch,
  stickyHeaderHeight,
  mergePreventReasons
}) => {
  const { t } = useTranslation("plugins");
  const { data: comments, isLoading, error } = useComments(repository, pullRequest);

  const fileContentFactory: FileContentFactory = (file: File) => {
    const baseUrl = `/repo/${repository.namespace}/${repository.name}/code/sources`;
    const sourceLink = {
      url: `${baseUrl}/${pullRequest && pullRequest.source && encodeURIComponent(pullRequest.source)}/${file.newPath}/`,
      label: t("scm-review-plugin.diff.jumpToSource")
    };
    const targetLink = pullRequest &&
      pullRequest.target && {
        url: `${baseUrl}/${encodeURIComponent(pullRequest.target)}/${file.oldPath}`,

        label: t("scm-review-plugin.diff.jumpToTarget")
      };

    const links = [];
    switch (file.type) {
      case "add":
        links.push(sourceLink);
        break;
      case "delete":
        if (targetLink) {
          links.push(targetLink);
        }
        break;
      default:
        if (targetLink) {
          links.push(sourceLink, targetLink);
        } else {
          links.push(sourceLink);
        }
    }

    const fileLinks = links.map(({ url, label }) => (
      <JumpToFileButton key={url} tooltip={label} link={url} icon="file-code" />
    ));

    const hasConflict = mergePreventReasons.find(reason => isFileContainedInMergePreventReason(reason, file));

    if (hasConflict) {
      const url = `/repo/${repository.namespace}/${repository.name}/pull-request/${
        pullRequest.id
      }/conflicts#${getDiffAnchorId(file)}`;
      return [
        ...fileLinks,
        <JumpToFileButton
          key={url}
          tooltip={t("scm-review-plugin.diff.jumpToConflict")}
          link={url}
          icon="fas fa-exclamation-triangle"
          color="warning"
        />
      ];
    }

    return fileLinks;
  };

  const diffUrl = createDiffUrl(repository, source, target);
  if (!diffUrl) {
    return <Notification type="danger">{t("scm-review-plugin.diff.notSupported")}</Notification>;
  } else if (isLoading) {
    return <Loading />;
  } else if (error) {
    return <ErrorNotification error={error} />;
  } else {
    const createLink = (comments?._links?.create as Link)?.href || undefined;
    const createWithImagesLink = (comments?._links?.createWithImages as Link)?.href || undefined;
    return (
      <Diff
        repository={repository}
        pullRequest={pullRequest}
        comments={comments}
        diffUrl={diffUrl}
        createLink={createLink}
        createLinkWithImages={createWithImagesLink}
        reviewedFiles={pullRequest?.markedAsReviewed || []}
        fileContentFactory={fileContentFactory}
        sourceBranch={sourceBranch}
        stickyHeaderHeight={stickyHeaderHeight}
      />
    );
  }
};

function isFileContainedInMergePreventReason(r: MergePreventReason, file: File) {
  return (
    r.affectedPaths && (r.affectedPaths?.indexOf(file.newPath) >= 0 || r.affectedPaths?.indexOf(file.oldPath) >= 0)
  );
}

export default DiffRoute;
