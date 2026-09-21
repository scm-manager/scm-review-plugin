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

import { DiffEventContext } from "@scm-manager/ui-components";
import { Location } from "../types/PullRequest";
import { createHunkId, createInlineLocation } from "./locations";

export type InlineEditorState = Record<string, Record<string, Location>>;

export const addInlineEditor = (state: InlineEditorState, context: DiffEventContext): InlineEditorState => {
  const hunkId = createHunkId(context);

  return {
    ...state,
    [hunkId]: {
      ...state[hunkId],
      [context.changeId]: createInlineLocation(context),
    },
  };
};

export const removeInlineEditor = (state: InlineEditorState, hunkId: string, changeId: string): InlineEditorState => {
  const hunkEditors = state[hunkId];
  if (!hunkEditors?.[changeId]) {
    return state;
  }

  const remainingEditors = { ...hunkEditors };
  delete remainingEditors[changeId];
  if (Object.keys(remainingEditors).length > 0) {
    return {
      ...state,
      [hunkId]: remainingEditors,
    };
  }

  const remainingHunks = { ...state };
  delete remainingHunks[hunkId];
  return remainingHunks;
};
