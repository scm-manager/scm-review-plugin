/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React, { HTMLAttributes } from "react";
import { Result } from "../types/EngineConfig";

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
      <i {...rest} className={`fas fa-${size} has-text-${color} fa-${icon} is-relative`} ref={ref} />
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

export default StatusIcon;
