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
import { Button, Icon } from "@scm-manager/ui-core";
import styled from "styled-components";

type Props = {
  title: string;
  icon: string;
  onClick: () => void;
};

const ToolbarIcon: FC<Props> = ({ title, icon, onClick }) => (
  <Button
    style={{ backgroundColor: "#00000000", textDecoration: "none", margin: 0 }}
    className="level-item is-link p-0"
    onClick={onClick}
    aria-label={title}
    variant="tertiary"
  >
    <Icon>{icon}</Icon>
  </Button>
);

export default ToolbarIcon;
