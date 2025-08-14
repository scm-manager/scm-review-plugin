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
import { useTranslation } from "react-i18next";
import { Conflict, MergePreventReason, PullRequest } from "./types/PullRequest";
import { DiffFile, File, useScrollToElement } from "@scm-manager/ui-components";
import { Notification, ErrorNotification, Loading } from "@scm-manager/ui-core";
import { FileChangeType, Repository } from "@scm-manager/ui-types";
import { usePullRequestConflicts } from "./pullRequest";
// @ts-ignore
import parser from "gitdiff-parser";
import ManualMergeInformation from "./ManualMergeInformation";
import { useLocation } from "react-router-dom";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  mergePreventReasons: MergePreventReason[];
};

const MergeConflicts: FC<Props> = ({ repository, pullRequest, mergePreventReasons }) => {
  const [t] = useTranslation("plugins");
  const [mergeInformation, setMergeInformation] = useState(false);

  const { data, error, isLoading } = usePullRequestConflicts(repository, pullRequest);

  const location = useLocation();
  const [anchorContentRef, setAnchorContentRef] = useState<HTMLElement | null>();

  useScrollToElement(anchorContentRef, () => location.hash, location.hash);

  const getTypeLabel = (type: string) => {
    return t("scm-review-plugin.conflicts.types." + type);
  };

  const createDiffComponent = (conflict: Conflict, index: number) => {
    if (conflict.diff) {
      const parsedDiff = parser.parse(conflict.diff);
      return parsedDiff
        .map((file: File) => ({ ...file, type: getTypeLabel(conflict.type) }))
        .map((file: File) => <DiffFile markConflicts={true} file={file} sideBySide={false} key={index} />);
    } else {
      return (
        <DiffFile
          file={{ hunks: [], newPath: conflict.path, type: getTypeLabel(conflict.type) as FileChangeType }}
          sideBySide={false}
          key={index}
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
      {mergePreventReasons &&
        mergePreventReasons.map(reason => {
          return (
            <Notification type="warning">
              <div className="content">
                <b>{t(`scm-review-plugin.conflicts.hint.${reason.type}.header`)}</b>
                <p>
                  <a onClick={() => setMergeInformation(true)}>
                    {t(`scm-review-plugin.conflicts.hint.${reason.type}.text`)}
                  </a>
                </p>
              </div>
            </Notification>
          );
        })}
      <div ref={setAnchorContentRef}>
        {data!.conflicts.map((conflict: Conflict, index) => createDiffComponent(conflict, index))}
      </div>
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
