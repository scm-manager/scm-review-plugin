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
import { useTranslation } from "react-i18next";
import ReducedMarkdownView from "../ReducedMarkdownView";
import { Checkbox } from "@scm-manager/ui-core";
import { DEFAULT_ADDITIONAL_INFOS, DEFAULT_PROPERTIES, PullRequestChange } from "./ChangesTypes";
import { ChangesetId } from "@scm-manager/ui-components";
import { Changeset, Repository } from "@scm-manager/ui-types";

type ChangeProps = {
  change: PullRequestChange;
};

type Props = ChangeProps & {
  repository: Repository;
};

const BranchEditRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue || !change.currentValue) {
    throw new Error("Invalid edit change. Previous or current value is undefined");
  }

  return (
    <div className="box">
      <ReducedMarkdownView
        content={t("scm-review-plugin.pullRequest.changes.changedTargetBranch", {
          previous: change.previousValue,
          current: change.currentValue
        })}
      />
    </div>
  );
};

const MarkdownContentEditedRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue || !change.currentValue) {
    throw new Error("Invalid edit change. Previous or current value is undefined");
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.previousValue")}</p>
      <div className="box">
        <ReducedMarkdownView content={change.previousValue} />
      </div>
      <p>{t("scm-review-plugin.pullRequest.changes.currentValue")}</p>
      <div className="box">
        <ReducedMarkdownView content={change.currentValue} />
      </div>
    </>
  );
};

const ShouldSourceBranchBeDeletedAfterMergeEditRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue || !change.currentValue) {
    throw new Error("Invalid edit change. Previous or current value is undefined");
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.previousValue")}</p>
      <div className="box">
        <Checkbox
          label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
          checked={change.previousValue === "true"}
          readOnly={true}
        />
      </div>
      <p>{t("scm-review-plugin.pullRequest.changes.currentValue")}</p>
      <div className="box">
        <Checkbox
          label={t("scm-review-plugin.showPullRequest.mergeModal.deleteSourceBranch.help")}
          checked={change.currentValue === "true"}
          readOnly={true}
        />
      </div>
    </>
  );
};

const ReviewerEditRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.additionalInfo) {
    throw new Error("Invalid reviewer edit change. Because additionalInfo is missing");
  }

  if (change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.CURRENT_APPROVED] === "true") {
    return (
      <p className="box">
        {t("scm-review-plugin.pullRequest.changes.reviewer.approved", {
          username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.CURRENT_USERNAME]
        })}
      </p>
    );
  }

  return (
    <p className="box">
      {t("scm-review-plugin.pullRequest.changes.reviewer.disapproved", {
        username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.CURRENT_USERNAME]
      })}
    </p>
  );
};

const CommentTypeEditedRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.additionalInfo || !change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.COMMENT_VALUE]) {
    throw new Error("Invalid comment type change. The comment is missing in additionalInfo");
  }

  const content: string | undefined =
    change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.IS_SYSTEM_COMMENT] === "true"
      ? t(`scm-review-plugin.comment.systemMessage.${change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.COMMENT_VALUE]}`)
      : change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.COMMENT_VALUE];

  if (!content) {
    throw new Error("Invalid comment type change. The comment content could not be evaluated");
  }

  return (
    <div className="box">
      <ReducedMarkdownView content={content} />
    </div>
  );
};

const PullRequestStatusEditRenderer: FC<ChangeProps> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue || !change.currentValue) {
    throw new Error("Invalid edit change. Previous or current value is undefined");
  }

  const statusChange = t("scm-review-plugin.pullRequest.changes.changedPrStatus", {
    previous: t(`scm-review-plugin.pullRequest.statusLabel.${change.previousValue}`),
    current: t(`scm-review-plugin.pullRequest.statusLabel.${change.currentValue}`)
  });

  const rejectionCause =
    change.additionalInfo && change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.REJECTION_CAUSE]
      ? t(
          `scm-review-plugin.pullRequest.changes.rejectionCause.${
            change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.REJECTION_CAUSE]
          }`
        )
      : null;

  return (
    <div className="box">
      <ReducedMarkdownView content={`${statusChange}${rejectionCause ? rejectionCause : ""}`} />
    </div>
  );
};

const SourceBranchRevisionEditRenderer: FC<Props> = ({ change, repository }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue || !change.currentValue) {
    throw new Error("Invalid edit change. Previous or current value is undefined");
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.revision.previous")}</p>
      <p className="box">
        <ChangesetId repository={repository} changeset={{ id: change.previousValue } as Changeset} />
      </p>
      <p>{t("scm-review-plugin.pullRequest.changes.revision.current")}</p>
      <p className="box">
        <ChangesetId repository={repository} changeset={{ id: change.currentValue } as Changeset} />
      </p>
    </>
  );
};

const EditRenderer: FC<Props> = ({ change, repository }) => {
  const { t } = useTranslation("plugins");

  if (change.property === DEFAULT_PROPERTIES.TARGET_BRANCH) {
    return <BranchEditRenderer change={change} />;
  }

  if (
    change.property === DEFAULT_PROPERTIES.DESCRIPTION ||
    change.property === DEFAULT_PROPERTIES.COMMENT ||
    change.property === DEFAULT_PROPERTIES.REPLY ||
    change.property === DEFAULT_PROPERTIES.TASK
  ) {
    return <MarkdownContentEditedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.DELETE_SOURCE_BRANCH_AFTER_MERGE) {
    return <ShouldSourceBranchBeDeletedAfterMergeEditRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.REVIEWER) {
    return <ReviewerEditRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.PR_STATUS) {
    return <PullRequestStatusEditRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.COMMENT_TYPE) {
    return <CommentTypeEditedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.SOURCE_BRANCH_REVISION) {
    return <SourceBranchRevisionEditRenderer change={change} repository={repository} />;
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.previousValue")}</p>
      <p className="box">{change.previousValue}</p>
      <p>{t("scm-review-plugin.pullRequest.changes.currentValue")}</p>
      <p className="box">{change.currentValue}</p>
    </>
  );
};

export default EditRenderer;
