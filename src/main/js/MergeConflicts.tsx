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
import { useTranslation } from "react-i18next";
import { Conflict, PullRequest } from "./types/PullRequest";
import { DiffFile, ErrorNotification, File, Loading, Notification } from "@scm-manager/ui-components";
import { FileChangeType, Repository } from "@scm-manager/ui-types";
import { usePullRequestConflicts } from "./pullRequest";
// @ts-ignore
import parser from "gitdiff-parser";
import ManualMergeInformation from "./ManualMergeInformation";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const MergeConflicts: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const [mergeInformation, setMergeInformation] = useState(false);

  const { data, error, isLoading } = usePullRequestConflicts(repository, pullRequest);

  const getTypeLabel = (type: string) => {
    return t("scm-review-plugin.conflicts.types." + type);
  };

  const createDiffComponent = (conflict: Conflict) => {
    if (conflict.diff) {
      const parsedDiff = parser.parse(conflict.diff);
      return parsedDiff
        .map((file: File) => ({ ...file, type: getTypeLabel(conflict.type) }))
        .map((file: File) => <DiffFile markConflicts={true} file={file} sideBySide={false} />);
    } else {
      return (
        <DiffFile
          file={{ hunks: [], newPath: conflict.path, type: getTypeLabel(conflict.type) as FileChangeType }}
          sideBySide={false}
        />
      );
    }
  };

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (isLoading) {
    return <Loading />;
  }

  return (
    <>
      <Notification type={"warning"}>
        <div className="content">
          <b>{t("scm-review-plugin.conflicts.hint.header")}</b>
          <p>
            <a onClick={() => setMergeInformation(true)}>{t("scm-review-plugin.conflicts.hint.text")}</a>
          </p>
        </div>
      </Notification>
      {data!.conflicts.map(conflict => createDiffComponent(conflict))}
      <ManualMergeInformation
        showMergeInformation={mergeInformation}
        repository={repository}
        pullRequest={pullRequest}
        onClose={() => setMergeInformation(false)}
      />
    </>
  );
};

export default MergeConflicts;
