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
