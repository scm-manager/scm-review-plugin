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
    return <DiffButton onClick={markFile} tooltip={t("scm-review-plugin.diff.markReviewed")} icon="clipboard" />;
  }
};

export default MarkReviewedButton;
