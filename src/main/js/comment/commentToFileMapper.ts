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
