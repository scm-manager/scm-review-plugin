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

import {
  createFileLocation,
  createHunkIdFromLocation,
  createInlineLocation,
  isInlineLocation,
  createChangeIdFromLocation
} from "./locations";
import { Change, DiffEventContext } from "@scm-manager/ui-components";
import { Location } from "../types/PullRequest";

describe("test createChangeIdFromLocation", () => {
  const locationHead: Location = {
    file: "a.txt",
    hunk: "@@ -0,0 +1,100 @@"
  };

  it("should create change id from normal/unchanged location", () => {
    const location: Location = {
      ...locationHead,
      newLineNumber: 42,
      oldLineNumber: 21
    };

    const changeId = createChangeIdFromLocation(location);
    expect(changeId).toBe("N21");
  });

  it("should create change id from inserted location", () => {
    const location: Location = {
      ...locationHead,
      newLineNumber: 42
    };

    const changeId = createChangeIdFromLocation(location);
    expect(changeId).toBe("I42");
  });

  it("should create change id from deleted location", () => {
    const location: Location = {
      ...locationHead,
      oldLineNumber: 21
    };

    const changeId = createChangeIdFromLocation(location);
    expect(changeId).toBe("D21");
  });

  it("should create change id from deleted location", () => {
    expect(() => createChangeIdFromLocation(locationHead)).toThrowError("at least one line number has to be set");
  });
});

describe("test createHunkIdFromLocation", () => {
  it("should create hunk id", () => {
    const location: Location = {
      file: "a.txt",
      hunk: "@@ -0,0 +1,100 @@",
      newLineNumber: 42
    };

    const hunkId = createHunkIdFromLocation(location);
    expect(hunkId).toBe("a.txt_@@ -0,0 +1,100 @@");
  });

  it("should throw an error for locations without hunk", () => {
    const location: Location = {
      file: "a.txt",
      newLineNumber: 42
    };

    const fn = () => createHunkIdFromLocation(location);
    expect(fn).toThrowError("only locations with a hunk could be used");
  });
});

describe("test isInlineLocation", () => {
  it("should return true", () => {
    const location: Location = {
      file: "a.txt",
      hunk: "@@ -0,0 +1,100 @@",
      newLineNumber: 42
    };
    expect(isInlineLocation(location)).toBe(true);
  });

  it("should return false", () => {
    const location: Location = {
      file: "a.txt"
    };
    expect(isInlineLocation(location)).toBe(false);
  });
});

describe("test createFileLocation", () => {
  const createAddedFileContext = () => {
    return {
      file: {
        oldPath: "/dev/null",
        newPath: "a.txt",
        type: "add"
      }
    };
  };

  const createDeletedFileContext = () => {
    return {
      file: {
        oldPath: "a.txt",
        newPath: "/dev/null",
        type: "delete"
      }
    };
  };

  it("should not set hunk and line numbers", () => {
    const location = createFileLocation(createAddedFileContext());

    expect(location.hunk).toBeUndefined();
    expect(location.oldLineNumber).toBeUndefined();
    expect(location.newLineNumber).toBeUndefined();
  });

  it("should create location for added file", () => {
    const location = createFileLocation(createAddedFileContext());

    expect(location.file).toBe("a.txt");
  });

  it("should create location for deleted file", () => {
    const location = createFileLocation(createDeletedFileContext());

    expect(location.file).toBe("a.txt");
  });
});

describe("test createInlineLocation", () => {
  const createContext = (change: Change) => {
    return {
      file: {
        oldPath: "/dev/null",
        newPath: "a.txt",
        type: "add"
      },
      hunk: {
        content: "@@ -0,0 +1,100 @@"
      },
      change,
      changeId: "irrelevant"
    };
  };

  it("should map hunk and file", () => {
    const context: DiffEventContext = createContext({
      content: "i'm a added line",
      type: "insert",
      lineNumber: 42
    });

    const location = createInlineLocation(context);
    expect(location.file).toBe("a.txt");
    expect(location.hunk).toBe("@@ -0,0 +1,100 @@");
  });

  it("should map insert change to location", () => {
    const context: DiffEventContext = createContext({
      content: "i'm a added line",
      type: "insert",
      lineNumber: 42
    });

    const location = createInlineLocation(context);
    expect(location.oldLineNumber).toBeUndefined();
    expect(location.newLineNumber).toBe(42);
  });

  it("should map delete change to location", () => {
    const context: DiffEventContext = createContext({
      content: "i'm a deleted line",
      type: "delete",
      lineNumber: 42
    });

    const location = createInlineLocation(context);
    expect(location.oldLineNumber).toBe(42);
    expect(location.newLineNumber).toBeUndefined();
  });

  it("should map normal change to location", () => {
    const context: DiffEventContext = createContext({
      content: "i'm a deleted line",
      type: "normal",
      oldLineNumber: 21,
      newLineNumber: 42
    });

    const location = createInlineLocation(context);
    expect(location.oldLineNumber).toBe(21);
    expect(location.newLineNumber).toBe(42);
  });
});
