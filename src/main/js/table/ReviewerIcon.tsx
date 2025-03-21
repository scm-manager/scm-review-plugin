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
