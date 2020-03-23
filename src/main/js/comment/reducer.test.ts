///
/// MIT License
///
/// Copyright (c) 2020-present Cloudogu GmbH and Contributors
///
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
///
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
///
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.
///

import reducer, { initialState, State } from "./reducer";
import {
  createComment,
  createReply,
  deleteComment,
  deleteReply,
  fetchAll,
  updateComment,
  updateReply
} from "./actiontypes";
import { Comment } from "../types/PullRequest";

describe("test reducer", () => {
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
      date: new Date().toDateString(),
      replies: [],
      _links: {}
    };
  };

  it("should handle fetched comments", () => {
    const comments = [createTestComment("1", "one"), createTestComment("2", "two")];
    const state = reducer(initialState, fetchAll(comments));
    expect(state.length).toBe(2);
  });

  it("should append new comment", () => {
    const comment = createTestComment("1", "one");
    const state = reducer(initialState, createComment(comment));
    expect(state.length).toBe(1);
  });

  it("should append new comment to existing ones", () => {
    const state: State = [createTestComment("1", "one")];
    const two = createTestComment("2", "two");
    const nextState = reducer(state, createComment(two));
    expect(nextState.length).toBe(2);
  });

  it("should delete existing comment", () => {
    const one = createTestComment("1", "one");
    const state: State = [one];

    const nextState = reducer(state, deleteComment(one));
    expect(nextState.length).toBe(0);
  });

  it("should update existing comments", () => {
    const one = createTestComment("1", "one");
    const state: State = [one];
    const two = createTestComment("1", "New one");
    const nextState = reducer(state, updateComment(two));
    expect(nextState[0].comment).toBe("New one");
  });

  const findReply = (comment: Comment, id: string) => {
    if (comment._embedded && comment._embedded.replies) {
      return comment._embedded.replies.find((r: Comment) => r.id === id);
    }
  };

  it("should append reply to comment", () => {
    const state: State = [createTestComment("1", "one")];

    const two = createTestComment("2", "two");
    const nextState = reducer(state, createReply("1", two));
    expect(findReply(nextState[0], "2")).toBeDefined();
  });

  it("should delete reply", () => {
    const one = createTestComment("1", "one");
    const two = createTestComment("2", "two");
    one._embedded = {
      replies: [two]
    };
    const nextState = reducer([one], deleteReply("1", two));
    expect(findReply(nextState[0], "2")).toBeUndefined();
  });

  it("should update reply", () => {
    const one = createTestComment("1", "one");
    const two = createTestComment("2", "two");
    one._embedded = {
      replies: [two]
    };
    const state: State = [one];

    const newTwo = createTestComment("2", "New Two");
    const nextState = reducer(state, updateReply("1", newTwo));
    expect(findReply(nextState[0], "2").comment).toBe("New Two");
  });
});
