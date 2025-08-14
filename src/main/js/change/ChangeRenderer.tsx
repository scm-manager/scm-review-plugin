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
import { ChangeType } from "./util";
import { PullRequestChange } from "./ChangesTypes";
import CreatedRenderer from "./CreatedRenderer";
import RemovedRenderer from "./RemovedRenderer";
import EditRenderer from "./EditRenderer";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  changeType: ChangeType;
  change: PullRequestChange;
  repository: Repository;
};

const ChangeRenderer: FC<Props> = ({ changeType, change, repository }) => {
  if (changeType === "edit") {
    return <EditRenderer change={change} repository={repository} />;
  }

  if (changeType === "removed") {
    return <RemovedRenderer change={change} />;
  }

  return <CreatedRenderer change={change} />;
};

export default ChangeRenderer;
