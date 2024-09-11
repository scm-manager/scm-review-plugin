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
import { Icon } from "@scm-manager/ui-components";

type Props = {
  title: string;
  icon: string;
  onClick: () => void;
};

const ToolbarIcon: FC<Props> = ({ title, icon, onClick }) => (
  <a className="level-item" onClick={onClick} title={title}>
    <span className="icon is-small">
      <Icon color="link" name={icon} />
    </span>
  </a>
);

export default ToolbarIcon;
