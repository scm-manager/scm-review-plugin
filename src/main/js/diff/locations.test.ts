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
