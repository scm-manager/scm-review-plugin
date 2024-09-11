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

import React, { HTMLAttributes } from "react";
import { Result } from "../types/EngineConfig";
import { Icon } from "@scm-manager/ui-buttons";

type Props = HTMLAttributes<HTMLElement> & {
  color?: string;
  icon?: string;
  size?: string;
  titleType?: string;
  title?: string;
};

const StatusIcon = React.forwardRef<HTMLElement, Props>(
  ({ color = "secondary", icon = "circle-notch", size = "lg", titleType, title, ...rest }, ref) => (
    <>
      <Icon ref={ref} {...rest}>{`${icon} has-text-${color} fa-${size}`}</Icon>
      {(titleType || title) && (
        <span
          style={{
            paddingLeft: "0.5rem"
          }}
        >
          {titleType && <strong>{titleType}: </strong>}
          {title ? title : null}
        </span>
      )}
    </>
  )
);

export const getColor = (results: Result[]) => {
  if (results && results.length) {
    if (results.some(it => it.failed)) {
      return "danger";
    } else {
      return "success";
    }
  } else {
    return "secondary";
  }
};

export const getIcon = (results: Result[]) => {
  if (results && results.length) {
    if (results.some(it => it.failed)) {
      return "times-circle";
    } else {
      return "check-circle";
    }
  } else {
    return "circle-notch";
  }
};

export const getTitle = (results: Result[]) => {
  if (results && results.length) {
    if (results.some(it => it.failed)) {
      return "fail";
    } else {
      return "success";
    }
  } else {
    return "pending";
  }
};

export default StatusIcon;
