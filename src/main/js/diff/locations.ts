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

import { BaseContext, DiffEventContext, diffs } from "@scm-manager/ui-components";
import { Location } from "../types/PullRequest";

export function createHunkId(context: BaseContext): string {
  return diffs.getPath(context.file) + "_" + context.hunk.content;
}

export function createHunkIdFromLocation(location?: Location): string {
  if (!location?.hunk) {
    throw new Error("only locations with a hunk could be used");
  }
  return location.file + "_" + location.hunk;
}

export function isInlineLocation(location?: Location): boolean {
  return !!location?.hunk;
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

type LineNumbers = { oldLineNumber?: number; newLineNumber?: number };

export const evaluateLineNumbersForChangeId = (changeId: string): LineNumbers => {
  const changeType = changeId.substr(0, 1);
  const lineNumber = parseInt(changeId.substr(1, changeId.length - 1));
  if (changeType === "N") {
    return { oldLineNumber: lineNumber, newLineNumber: lineNumber };
  }
  if (changeType === "I") {
    return { newLineNumber: lineNumber };
  }
  if (changeType === "D") {
    return { oldLineNumber: lineNumber };
  }
  return {};
};

export function escapeWhitespace(path: string) {
  return path?.toLowerCase().replace(/\W/g, "-");
}
