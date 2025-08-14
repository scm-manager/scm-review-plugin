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
import classNames from "classnames";
import BranchTag from "./BranchTag";
import CopyToClipboardButton from "./CopyToClipboardButton";

type Props = {
  className?: string;
  wrapper?: React.ElementType;
  source: string;
  target: string;
};

const SourceTargetBranchDisplay: FC<Props> = ({ className, children, source, target, wrapper = "div" }) => {
  const content = (
    <>
      <BranchTag label={source} title={source}>
        <CopyToClipboardButton text={source} />
      </BranchTag>
      <i className="fas fa-long-arrow-alt-right m-1" />
      <BranchTag label={target} title={target} />
    </>
  );
  return React.createElement(
    wrapper,
    { className: classNames(className, "is-flex", "is-align-items-center", "is-flex-wrap-wrap") },
    content,
    children,
  );
};

export default SourceTargetBranchDisplay;
