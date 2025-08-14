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
import { DEFAULT_ADDITIONAL_INFOS, DEFAULT_PROPERTIES, PullRequestChange } from "./ChangesTypes";

type Props = {
  change: PullRequestChange;
};

const ReviewerAddedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.additionalInfo) {
    throw new Error("Invalid add reviewer change. Because additionalInfo is missing");
  }

  return (
    <p className="box">
      {`${t("scm-review-plugin.pullRequest.changes.reviewer.added", {
        username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.CURRENT_USERNAME]
      })} ${t(
        `scm-review-plugin.pullRequest.changes.reviewer.${
          change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.CURRENT_APPROVED] === "true"
            ? "addedWithApproval"
            : "addedWithoutApproval"
        }`
      )}`}
    </p>
  );
};

const MarkdownContentAddedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.currentValue) {
    throw new Error("Invalid create change. Current value is undefined");
  }

  let content = change.currentValue;
  if (
    change.property === DEFAULT_PROPERTIES.COMMENT &&
    change.additionalInfo &&
    change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.IS_SYSTEM_COMMENT] === "true"
  ) {
    content = t(`scm-review-plugin.comment.systemMessage.${change.currentValue}`);
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.added")}</p>
      <div className="box">
        <ReducedMarkdownView content={content} />
      </div>
    </>
  );
};

const ReviewMarkAddedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.currentValue) {
    throw new Error("Invalid create change. Current value is undefined");
  }

  if (!change.additionalInfo) {
    throw new Error("Invalid add review mark change. Because additionalInfo is missing");
  }

  return (
    <>
      <div className="box">
        <ReducedMarkdownView
          content={t("scm-review-plugin.pullRequest.changes.reviewMark.added", {
            username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.USER],
            file: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.FILE]
          })}
        />
      </div>
    </>
  );
};

const SubscriberAddedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.currentValue) {
    throw new Error("Invalid created change. current value is undefined");
  }

  return (
    <>
      <p className="box">
        {t("scm-review-plugin.pullRequest.changes.subscriber.added", { username: change.currentValue })}
      </p>
    </>
  );
};

const CreatedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (change.property === DEFAULT_PROPERTIES.REVIEWER) {
    return <ReviewerAddedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.SUBSCRIBER) {
    return <SubscriberAddedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.REVIEW_MARKS) {
    return <ReviewMarkAddedRenderer change={change} />;
  }

  if (
    change.property === DEFAULT_PROPERTIES.COMMENT ||
    change.property === DEFAULT_PROPERTIES.REPLY ||
    change.property === DEFAULT_PROPERTIES.DESCRIPTION ||
    change.property === DEFAULT_PROPERTIES.TASK
  ) {
    return <MarkdownContentAddedRenderer change={change} />;
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.added")}</p>
      <p className="box">{change.currentValue}</p>
    </>
  );
};

export default CreatedRenderer;
