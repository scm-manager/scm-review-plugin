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

import React, { FC, useState } from "react";
import { DiffButton, ErrorNotification } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { useUpdateReviewMark } from "../pullRequest";
import { useTranslation } from "react-i18next";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  oldPath: string;
  newPath: string;
  setReviewed: (filepath: string, reviewed: boolean) => void;
  reviewedFiles: string[];
};

const MarkReviewedButton: FC<Props> = ({ repository, pullRequest, oldPath, newPath, setReviewed, reviewedFiles }) => {
  const [t] = useTranslation("plugins");
  const determinePath = () => {
    if (newPath !== "/dev/null") {
      return newPath;
    } else {
      return oldPath;
    }
  };

  const [marked, setMarked] = useState(reviewedFiles.some((markedFile: string) => markedFile === determinePath()));
  const { mark, unmark, error } = useUpdateReviewMark(repository, pullRequest, determinePath());

  const reposition = (id: string) => {
    const element = document.getElementById(id);
    // Prevent skipping diffs on collapsing long ones because of the sticky header
    // We jump to the start of the diff and afterwards go slightly up to show the diff header right under the page header
    // Only scroll if diff is not collapsed and is using the "sticky" mode
    const pageHeaderSize = 50;
    if (element && element.getBoundingClientRect().top < pageHeaderSize) {
      element.scrollIntoView();
      window.scrollBy(0, -pageHeaderSize);
    }
  };

  const getAnchorId = (newPath: string, oldPath: string) => {
    const path = newPath ? newPath : oldPath;
    return path?.toLowerCase().replace(/\W/g, "-");
  };

  const markFile = () => {
    mark();
    setReviewed(determinePath(), true);
    setMarked(true);
  };

  const unmarkFile = () => {
    unmark();
    setReviewed(determinePath(), false);
    setMarked(false);
  };

  if (!pullRequest?._links?.reviewMark) {
    return null;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (marked) {
    return (
      <DiffButton onClick={unmarkFile} tooltip={t("scm-review-plugin.diff.markNotReviewed")} icon="clipboard-check" />
    );
  } else {
    const anchorId = getAnchorId(newPath, oldPath);
    return (
      <DiffButton
        id={anchorId}
        onClick={() => {
          markFile();
          reposition(anchorId);
        }}
        tooltip={t("scm-review-plugin.diff.markReviewed")}
        icon="clipboard"
      />
    );
  }
};

export default MarkReviewedButton;
