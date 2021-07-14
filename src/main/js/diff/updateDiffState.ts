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
import { Comments, Location } from "../types/PullRequest";
import { createChangeIdFromLocation } from "./locations";
import { DiffState } from "./Diff";
import { Dispatch, SetStateAction } from "react";

type Files = { [key: string]: { comments: string[] } };

type Lines = { [key: string]: { [key: string]: { comments: string[]; location: Location } } };

export const updateDiffStateForComments = (
  comments: Comments,
  diffState: DiffState,
  setDiffState: Dispatch<SetStateAction<DiffState>>
) => {
  const files: Files = {};
  const lines: Lines = {};
  for (const comment of comments._embedded.pullRequestComments) {
    const location = comment.location;
    if (comment.id && location && !location?.newLineNumber && !location?.oldLineNumber) {
      const preUpdateFiles = files[location.file];
      if (preUpdateFiles && preUpdateFiles.comments) {
        files[location.file] = { ...preUpdateFiles, comments: [...preUpdateFiles?.comments, comment.id] };
      } else {
        files[location.file] = { comments: [comment.id] };
      }
    }
    if (location?.oldLineNumber || location?.newLineNumber) {
      const changeId = createChangeIdFromLocation(location);
      const lineId = location.file + "_" + location.hunk;
      const preUpdateLines = lines[lineId];
      if (preUpdateLines) {
        const preUpdateChanges = lines[lineId][changeId];
        if (preUpdateChanges) {
          lines[lineId] = {
            ...preUpdateLines,
            [changeId]: { ...[preUpdateChanges], comments: [...preUpdateChanges?.comments, comment.id!], location }
          };
        } else {
          lines[lineId] = {
            ...preUpdateLines,
            [changeId]: { comments: [comment.id!], location }
          };
        }
      } else {
        lines[lineId] = {
          [changeId]: { comments: [comment.id!], location: location }
        };
      }
    }
  }
  setDiffState((currentState: DiffState) => ({
    ...currentState,
    comments: comments?._embedded.pullRequestComments,
    files,
    lines
  }));
};
