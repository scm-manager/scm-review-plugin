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
import { addInlineEditor, removeInlineEditor } from "./inlineEditorState";

const hunk = {
  content: "@@ -23,2 +24,2 @@",
  changes: [],
};

const file = {
  oldPath: "a.txt",
  newPath: "a.txt",
  type: "modify" as const,
  oldEndingNewLine: true,
  newEndingNewLine: true,
};

const context: DiffEventContext = {
  file,
  hunk,
  changeId: "N23",
  change: {
    content: "unchanged line",
    type: "normal",
    oldLineNumber: 23,
    newLineNumber: 24,
  },
};

const hunkId = `${file.newPath}_${hunk.content}`;

describe("inline editor state", () => {
  it("keeps both line numbers while indexing an unchanged line by its diff change id", () => {
    const state = addInlineEditor({}, context);

    expect(state[hunkId]["N23"]).toEqual({
      file: "a.txt",
      hunk: hunk.content,
      oldLineNumber: 23,
      newLineNumber: 24,
    });
  });

  it("does not create duplicate editors for the same change", () => {
    const state = addInlineEditor(addInlineEditor({}, context), context);

    expect(Object.keys(state[hunkId])).toEqual(["N23"]);
  });

  it("removes the editor using its diff change id", () => {
    const state = addInlineEditor({}, context);

    expect(removeInlineEditor(state, hunkId, "N23")).toEqual({});
  });
});
