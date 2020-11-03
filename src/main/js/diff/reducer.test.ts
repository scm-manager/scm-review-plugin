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

import { Comment, Location } from "../types/PullRequest";
import {
  closeEditor,
  createComment,
  createReply,
  deleteComment,
  deleteReply,
  fetchAll,
  openEditor,
  updateComment,
  updateReply
} from "../comment/actiontypes";
import reducer, {createInitialState, markAsReviewed, State, unmarkAsReviewed} from "./reducer";

describe("diff comment reducer tests", () => {
  const createTestComment = (id: string, comment: string): Comment => {
    return {
      id,
      comment,
      type: "test-comment",
      author: {
        id: "dirk",
        displayName: "Dirk Gently",
        mail: "dirk@gently.com"
      },
      systemComment: false,
      outdated: false,
      mentions: [],
      emergencyMerged: false,
      date: new Date().toDateString(),
      replies: [],
      _links: {}
    };
  };

  const createTestFileComment = (id: string, comment: string, file: string) => {
    const c = createTestComment(id, comment);
    c.location = {
      file: file
    };
    return c;
  };

  const createTestInlineComment = (id: string, comment: string, location: Location) => {
    const c = createTestComment(id, comment);
    c.location = location;
    return c;
  };

  it("should separate file and inline comments", () => {
    const comments = [
      createTestComment("one", "should be dropped"),
      createTestFileComment("two", "the file one", "file.txt"),
      createTestInlineComment("three", "the inline one", {
        file: "file.txt",
        hunk: "@@ -28,12 +27,10 @@",
        newLineNumber: 27
      })
    ];

    const state = reducer(createInitialState([]), fetchAll(comments));
    expect(state.files["file.txt"].comments.length).toBe(1);
    expect(state.files["file.txt"].comments[0]).toBe("two");

    expect(state.lines["file.txt_@@ -28,12 +27,10 @@"]["I27"].comments.length).toBe(1);
    expect(state.lines["file.txt_@@ -28,12 +27,10 @@"]["I27"].comments[0]).toBe("three");
    expect(state.comments["two"]).toBeDefined();
    expect(state.comments["three"]).toBeDefined();
  });

  it("should append file comment", () => {
    const prevState: State = {
      files: {
        "test.txt": {
          comments: ["one"]
        }
      },
      lines: {},
      comments: {
        one: createTestFileComment("one", "comment one", "test.txt")
      },
      reviewedFiles: []
    };

    const two = createTestFileComment("two", "comment two", "test.txt");
    const state = reducer(prevState, createComment(two));
    expect(state.files["test.txt"].comments.length).toBe(2);
    expect(state.comments["one"]).toBeDefined();
    expect(state.comments["two"]).toBeDefined();
  });

  it("should create new file comment", () => {
    const prevState: State = {
      files: {
        "a.txt": {
          comments: ["one"]
        }
      },
      lines: {},
      comments: {
        one: createTestFileComment("one", "comment one", "a.txt")
      },
      reviewedFiles: []
    };

    const two = createTestFileComment("two", "comment two", "b.txt");
    const state = reducer(prevState, createComment(two));
    expect(state.files["a.txt"].comments.length).toBe(1);
    expect(state.files["b.txt"].comments.length).toBe(1);
  });

  it("should append inline comment", () => {
    const location = {
      file: "a.txt",
      hunk: "@@ -28,12 +27,10 @@",
      newLineNumber: 27
    };
    const prevState: State = {
      files: {},
      lines: {
        "a.txt_@@ -28,12 +27,10 @@": {
          I27: {
            comments: ["one"],
            location
          }
        }
      },
      comments: {
        one: createTestInlineComment("one", "comment one", location)
      },
      reviewedFiles: []
    };

    const two = createTestInlineComment("two", "comment two", location);
    const state = reducer(prevState, createComment(two));
    const comments = state.lines["a.txt_@@ -28,12 +27,10 @@"]["I27"].comments;
    expect(comments.length).toBe(2);
  });

  it("should update file comment", () => {
    const prevState: State = {
      files: {
        "a.txt": {
          comments: ["one"]
        }
      },
      lines: {},
      comments: {
        one: createTestFileComment("one", "comment one", "a.txt")
      },
      reviewedFiles: []
    };

    const one = createTestFileComment("one", "the awesome one", "a.txt");
    const state = reducer(prevState, updateComment(one));
    expect(state.comments["one"].comment).toBe("the awesome one");
  });

  it("should delete file comment", () => {
    const one = createTestFileComment("one", "the awesome one", "a.txt");
    const prevState: State = {
      files: {
        "a.txt": {
          comments: ["one"]
        }
      },
      lines: {},
      comments: {
        one: one
      },
      reviewedFiles: []
    };

    const state = reducer(prevState, deleteComment(one));
    expect(state.files["a.txt"].comments.length).toBe(0);
    expect(state.comments["one"]).toBeUndefined();
  });

  it("should delete inline comment", () => {
    const location = {
      file: "a.txt",
      hunk: "@@ -28,12 +27,10 @@",
      newLineNumber: 27
    };
    const one = createTestInlineComment("one", "the awesome one", location);
    const prevState: State = {
      files: {},
      lines: {
        "a.txt_@@ -28,12 +27,10 @@": {
          I27: {
            comments: ["one"],
            location
          }
        }
      },
      comments: {
        one: one
      },
      reviewedFiles: []
    };

    const state = reducer(prevState, deleteComment(one));
    expect(state.lines["a.txt_@@ -28,12 +27,10 @@"]["I27"].comments.length).toBe(0);
    expect(state.comments["one"]).toBeUndefined();
  });

  it("should create reply", () => {
    const prevState: State = {
      files: {},
      lines: {},
      comments: {
        one: createTestFileComment("one", "comment one", "a.txt")
      },
      reviewedFiles: []
    };

    const two = createTestComment("two", "comment two");
    const state = reducer(prevState, createReply("one", two));
    const one = state.comments["one"];
    if (one && one._embedded && one._embedded.replies) {
      expect(one._embedded.replies.length).toBe(1);
    } else {
      fail("no replies available");
    }
  });

  it("should delete reply", () => {
    const one = createTestFileComment("one", "comment one", "a.txt");
    const two = createTestComment("two", "comment two");
    one._embedded = {
      replies: [two]
    };
    const prevState: State = {
      files: {},
      lines: {},
      comments: {
        one: one
      },
      reviewedFiles: []
    };

    const state = reducer(prevState, deleteReply("one", two));
    const stateOne = state.comments["one"];
    if (stateOne && stateOne._embedded && stateOne._embedded.replies) {
      expect(stateOne._embedded.replies.length).toBe(0);
    } else {
      fail("failed to access replies");
    }
  });

  it("should update reply", () => {
    const one = createTestFileComment("one", "comment one", "a.txt");
    const two = createTestComment("two", "comment two");
    one._embedded = {
      replies: [two]
    };
    const prevState: State = {
      files: {},
      lines: {},
      comments: {
        one: one
      },
      reviewedFiles: []
    };

    const updatedTwo = createTestComment("two", "updated two");

    const state = reducer(prevState, updateReply("one", updatedTwo));
    const stateOne = state.comments["one"];
    if (stateOne && stateOne._embedded && stateOne._embedded.replies) {
      expect(stateOne._embedded.replies[0].comment).toBe("updated two");
    } else {
      fail("failed to access replies");
    }
  });

  it("should set file edit state to true", () => {
    const prevState: State = {
      files: {},
      lines: {},
      comments: {},
      reviewedFiles: []
    };
    const location: Location = {
      file: "a.txt"
    };
    const state = reducer(prevState, openEditor(location));
    expect(state.files["a.txt"].editor).toBe(true);
  });

  it("should set file edit state to false", () => {
    const prevState: State = {
      files: {
        "a.txt": {
          comments: [],
          editor: true
        }
      },
      lines: {},
      comments: {},
      reviewedFiles: []
    };
    const location: Location = {
      file: "a.txt"
    };
    const state = reducer(prevState, closeEditor(location));
    expect(state.files["a.txt"].editor).toBe(false);
  });

  it("should set line edit state to true", () => {
    const prevState: State = {
      files: {},
      lines: {},
      comments: {},
      reviewedFiles: []
    };
    const location: Location = {
      file: "a.txt",
      hunk: "@@ -28,12 +27,10 @@",
      newLineNumber: 27
    };
    const state = reducer(prevState, openEditor(location));
    expect(state.lines["a.txt_@@ -28,12 +27,10 @@"]["I27"].editor).toBe(true);
  });

  it("should set line edit state to false", () => {
    const location: Location = {
      file: "a.txt",
      hunk: "@@ -28,12 +27,10 @@",
      newLineNumber: 27
    };

    const prevState: State = {
      files: {},
      lines: {
        "a.txt_@@ -28,12 +27,10 @@": {
          I27: {
            comments: [],
            editor: true,
            location
          }
        }
      },
      comments: {},
      reviewedFiles: []
    };

    const state = reducer(prevState, closeEditor(location));
    expect(state.lines["a.txt_@@ -28,12 +27,10 @@"]["I27"].editor).toBe(false);
  });

  it("should mark file as reviewed", () => {
    const filepath = "a.txt";

    const prevState: State = {
      files: {},
      lines: {},
      comments: {},
      reviewedFiles: []
    };

    const state = reducer(prevState, markAsReviewed(filepath));
    expect(state.reviewedFiles[0]).toBe("a.txt");
  });

  it("should unmark file as reviewed", () => {
    const filepath = "a.txt";

    const prevState: State = {
      files: {},
      lines: {},
      comments: {},
      reviewedFiles: ["a.txt"]
    };

    const state = reducer(prevState, unmarkAsReviewed(filepath));
    expect(state.reviewedFiles).toEqual([]);
  });
});
