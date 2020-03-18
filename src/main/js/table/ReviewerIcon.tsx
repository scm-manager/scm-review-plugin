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
import { Icon, Tooltip } from "@scm-manager/ui-components";
import { Reviewer } from "../types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";

type Props = WithTranslation & {
  reviewers?: Reviewer[];
};

const chooseIcon = (length: number): string => {
  switch (length) {
    case 1:
      return "user";
    case 2:
      return "user-friends";
    default:
      return "users";
  }
};

const createTooltipMessage = (reviewers: Reviewer[]) => {
  return reviewers
    .map(r => r.displayName)
    .map(n => `- ${n}`)
    .join("\n");
};

const ReviewerIcon: FC<Props> = ({ reviewers, t }) => {
  if (!reviewers || reviewers.length === 0) {
    return null;
  }

  const icon = chooseIcon(reviewers.length);
  const message = t("scm-review-plugin.pullRequest.reviewer") + ":\n" + createTooltipMessage(reviewers);
  return (
    <Tooltip location="top" message={message}>
      <Icon name={icon} />
    </Tooltip>
  );
};

export default withTranslation("plugins")(ReviewerIcon);
