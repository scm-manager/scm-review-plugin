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

import { BaseContext, DiffEventContext } from "@scm-manager/ui-components";
import { diffs } from "@scm-manager/ui-components";
import { Location } from "../types/PullRequest";

export function createHunkId(context: BaseContext): string {
  return diffs.getPath(context.file) + "_" + context.hunk.content;
}

export function createHunkIdFromLocation(location: Location): string {
  if (!location.hunk) {
    throw new Error("only locations with a hunk could be used");
  }
  return location.file + "_" + location.hunk;
}

export function isInlineLocation(location?: Location): boolean {
  return !!location && !!location.hunk;
}

export function createFileLocation(context: BaseContext): Location {
  return {
    file: diffs.getPath(context.file)
  };
}

export function createInlineLocation(context: DiffEventContext): Location {
  const change = context.change;

  const location: Location = {
    file: diffs.getPath(context.file),
    hunk: context.hunk.content
  };

  switch (change.type) {
    case "insert":
      location.newLineNumber = change.lineNumber;
      break;
    case "delete":
      location.oldLineNumber = change.lineNumber;
      break;
    case "normal":
      location.newLineNumber = change.newLineNumber;
      location.oldLineNumber = change.oldLineNumber;
      break;
    default:
      throw new Error(`illegal change type ${change.type}`);
  }

  return location;
}

export function createChangeIdFromLocation(location: Location): string {
  if (location.oldLineNumber && location.newLineNumber) {
    return "N" + location.oldLineNumber;
  } else if (location.newLineNumber) {
    return "I" + location.newLineNumber;
  } else if (location.oldLineNumber) {
    return "D" + location.oldLineNumber;
  } else {
    throw new Error("at least one line number has to be set");
  }
}
