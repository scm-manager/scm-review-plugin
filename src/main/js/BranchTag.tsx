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
import styled from "styled-components";
import { Tag } from "@scm-manager/ui-components";

const ShortTag = styled(Tag).attrs(() => ({
  className: "is-medium",
  color: "light",
}))`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 25em;
  height: initial !important;
  display: inline-block !important;
  padding: 0.25em 0.75em;
`;

type Props = {
  label: string;
  title: string;
};

const BranchTag: FC<Props> = ({ label, title, children }) => {
  return (
    <span className="is-flex align-items-center">
      <ShortTag title={title}>
        {children ? (
          <div className="is-flex">
            {label}
            <div className="ml-2">{children}</div>
          </div>
        ) : (
          <>{label}</>
        )}
      </ShortTag>
    </span>
  );
};

export default BranchTag;
