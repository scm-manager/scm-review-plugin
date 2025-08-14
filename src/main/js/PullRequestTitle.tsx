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
import { PullRequest } from "./types/PullRequest";
import { useBinder } from "@scm-manager/ui-extensions";
import { SplitAndReplace, Replacement } from "@scm-manager/ui-components";

type Props = {
  pullRequest: PullRequest;
};

const PullRequestTitle: FC<Props> = ({ pullRequest }) => {
  const binder = useBinder();
  const value = pullRequest.title || "";

  const replacements: ((pullRequest: PullRequest, value: string) => Replacement[])[] = binder.getExtensions(
    "reviewPlugin.pullrequest.title.tokens",
    {
      pullRequest,
      value
    }
  );

  return <SplitAndReplace text={value} replacements={replacements.flatMap(r => r(pullRequest, value))} />;
};

export default PullRequestTitle;
