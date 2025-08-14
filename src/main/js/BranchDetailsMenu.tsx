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
import { Branch, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import { Menu } from "@scm-manager/ui-overlays";
import { Icon } from "@scm-manager/ui-buttons";

type Props = {
  repository: Repository;
  branch: Branch;
};

const BranchDetailsMenu: FC<Props> = ({ repository, branch }) => {
  const [t] = useTranslation("plugins");

  return (
    <Menu.Link
      to={`/repo/${repository.namespace}/${repository.name}/pull-requests/add/changesets/?source=${encodeURIComponent(
        branch.name
      )}`}
    >
      <Icon className="fa-rotate-180">code-branch</Icon>
      <span>{t("scm-review-plugin.branchDetails.pullRequest.create")}</span>
    </Menu.Link>
  );
};

export default BranchDetailsMenu;
