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
import { Tag } from "@scm-manager/ui-components";
import { evaluateTagColor } from "./pullRequest";
import { useTranslation } from "react-i18next";
import classNames from "classnames";

type Props = {
  status: string;
  emergencyMerged?: boolean;
  className?: string;
};

const PullRequestStatusTag: FC<Props> = ({ status, emergencyMerged = false, className }) => {
  const [t] = useTranslation("plugins");
  return (
    <Tag
      className={classNames("is-medium", className)}
      color={evaluateTagColor(status)}
      label={t("scm-review-plugin.pullRequest.statusLabel." + status)}
      icon={emergencyMerged ? "exclamation-triangle" : undefined}
    />
  );
};

export default PullRequestStatusTag;
