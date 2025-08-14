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

import { File } from "@scm-manager/ui-components";
import { Comment, ContextLine } from "../types/PullRequest";

export function mapCommentToFile(comment: Comment): File {
  const changes = comment.context.lines.map(line => mapContextLine(line));

  return {
    hunks: [
      {
        changes
      }
    ],
    newPath: comment.location.file
  };
}

function mapContextLine(contextLine: ContextLine) {
  return {
    content: contextLine.content,
    isInsert: !!contextLine.newLineNumber && !contextLine.oldLineNumber,
    isDelete: !contextLine.newLineNumber && !!contextLine.oldLineNumber,
    isNormal: !!contextLine.newLineNumber && !!contextLine.oldLineNumber,
    lineNumber: determineLineNumber(contextLine),
    newLineNumber: contextLine.newLineNumber,
    oldLineNumber: contextLine.oldLineNumber,
    type: determineType(contextLine)
  };
}

function determineType(contextLine: ContextLine) {
  if (contextLine.newLineNumber) {
    if (contextLine.oldLineNumber) {
      return "normal";
    } else {
      return "insert";
    }
  } else {
    return "delete";
  }
}

function determineLineNumber(contextLine: ContextLine) {
  if (contextLine.newLineNumber) {
    return contextLine.newLineNumber;
  } else {
    return contextLine.oldLineNumber;
  }
}
