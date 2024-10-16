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
import { DEFAULT_ADDITIONAL_INFOS, DEFAULT_PROPERTIES, PullRequestChange } from "./ChangesTypes";
import ReducedMarkdownView from "../ReducedMarkdownView";
import styled from "styled-components";

const BreakWordDiv = styled.div`
  word-break: break-word;
`;

type Props = {
  change: PullRequestChange;
};

const ReviewerRemovedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.additionalInfo) {
    throw new Error("Invalid remove reviewer change. Because additionalInfo is missing");
  }

  return (
    <p className="box">
      {t("scm-review-plugin.pullRequest.changes.reviewer.removed", {
        username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.PREVIOUS_USERNAME]
      })}
    </p>
  );
};

const MarkdownRemovedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue) {
    throw new Error("Invalid removed change. previous value is undefined");
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.removed")}</p>
      <div className="box">
        <ReducedMarkdownView content={change.previousValue} />
      </div>
    </>
  );
};

const SubscriberRemovedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue) {
    throw new Error("Invalid removed change. previous value is undefined");
  }

  return (
    <>
      <p className="box">
        {t("scm-review-plugin.pullRequest.changes.subscriber.removed", { username: change.previousValue })}
      </p>
    </>
  );
};

const ReviewMarkRemovedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (!change.previousValue) {
    throw new Error("Invalid remove change. Previous value is undefined");
  }

  if (!change.additionalInfo) {
    throw new Error("Invalid remove review mark change. Because additionalInfo is missing");
  }

  return (
    <>
      <BreakWordDiv className="box">
        <ReducedMarkdownView
          content={t("scm-review-plugin.pullRequest.changes.reviewMark.removed", {
            username: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.USER],
            file: change.additionalInfo[DEFAULT_ADDITIONAL_INFOS.FILE]
          })}
        />
      </BreakWordDiv>
    </>
  );
};

const RemovedRenderer: FC<Props> = ({ change }) => {
  const { t } = useTranslation("plugins");

  if (change.property === DEFAULT_PROPERTIES.REVIEWER) {
    return <ReviewerRemovedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.SUBSCRIBER) {
    return <SubscriberRemovedRenderer change={change} />;
  }

  if (change.property === DEFAULT_PROPERTIES.REVIEW_MARKS) {
    return <ReviewMarkRemovedRenderer change={change} />;
  }

  if (
    change.property === DEFAULT_PROPERTIES.COMMENT ||
    change.property === DEFAULT_PROPERTIES.REPLY ||
    change.property === DEFAULT_PROPERTIES.DESCRIPTION ||
    change.property === DEFAULT_PROPERTIES.TASK
  ) {
    return <MarkdownRemovedRenderer change={change} />;
  }

  return (
    <>
      <p>{t("scm-review-plugin.pullRequest.changes.removed")}</p>
      <p className="box">{change.previousValue}</p>
    </>
  );
};

export default RemovedRenderer;
