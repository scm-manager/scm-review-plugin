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
import { Action, ActionType, CommentAction, ReplyAction, FetchAllAction, EditorAction } from "../comment/actiontypes";
import { createChangeIdFromLocation, createHunkIdFromLocation, isInlineLocation } from "./locations";
import produce from "immer";

export type FileCommentState = {
  comments: string[];
  editor?: boolean;
};

export type FileCommentCollection = {
  // path
  [key: string]: FileCommentState;
};
export type LineCommentCollection = {
  // hunkid
  [key: string]: {
    // changeid
    [key: string]: {
      location: Location;
      comments: string[];
      editor?: boolean;
    };
  };
};

export type State = {
  files: FileCommentCollection;
  lines: LineCommentCollection;
  comments: { [key: string]: Comment };
  reviewedFiles: string[];
};

const initialState: State = {
  lines: {},
  files: {},
  comments: {},
  reviewedFiles: []
};

export const createInitialState = (reviewedFiles: string[]): State => {
  return {
    ...initialState,
    reviewedFiles
  };
};

type ReviewMarkActionType = "markAsReviewed" | "unmarkAsReviewed";

type ReviewMarkAction = {
  type: ReviewMarkActionType;
  filepath: string;
};

export type DiffAction = Action | ReviewMarkAction;
type DiffActionType = ActionType | ReviewMarkActionType;

export const markAsReviewed = (filepath: string): ReviewMarkAction => {
  return { type: "markAsReviewed", filepath };
};

export const unmarkAsReviewed = (filepath: string): ReviewMarkAction => {
  return { type: "unmarkAsReviewed", filepath };
};

type Reducer = (state: State, action: DiffAction) => State | undefined;

const addFileComments = (fileComments: FileCommentCollection, comment: Comment) => {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const file = location.file;
  const fileState = fileComments[file] || {
    comments: []
  };
  fileState.comments.push(comment.id);

  fileComments[file] = fileState;
};

const removeFileComment = (fileComment: FileCommentCollection, comment: Comment) => {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const file = fileComment[location.file];
  if (file) {
    const index = file.comments.indexOf(comment.id);
    if (index >= 0) {
      file.comments.splice(index, 1);
    }
  }
};

const addLineComments = (lineComments: LineCommentCollection, comment: Comment) => {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const hunkId = createHunkIdFromLocation(location);
  const changeId = createChangeIdFromLocation(location);
  const hunkComments = lineComments[hunkId] || {};

  const changeComments = hunkComments[changeId] || {
    location,
    comments: []
  };
  changeComments.comments.push(comment.id);
  hunkComments[changeId] = changeComments;

  lineComments[hunkId] = hunkComments;
};

const removeLineComment = (lineComments: LineCommentCollection, comment: Comment) => {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const hunk = lineComments[createHunkIdFromLocation(location)];
  if (hunk) {
    const change = hunk[createChangeIdFromLocation(location)];
    if (change) {
      const index = change.comments.indexOf(comment.id);
      if (index >= 0) {
        change.comments.splice(index, 1);
      }
    }
  }
};

const setEditState = (state: State, editor: boolean, location?: Location) => {
  if (!location) {
    throw new Error("location is required");
  }

  if (isInlineLocation(location)) {
    const lineComments = state.lines;

    const hunkId = createHunkIdFromLocation(location);
    const changeId = createChangeIdFromLocation(location);
    const hunkComments = lineComments[hunkId] || {};

    const changeComments = hunkComments[changeId] || {
      location,
      comments: []
    };
    changeComments.editor = editor;

    hunkComments[changeId] = changeComments;
    lineComments[hunkId] = hunkComments;
  } else {
    const fileComments = state.files;

    const file = location.file;
    const fileState = fileComments[file] || {
      comments: []
    };

    fileState.editor = editor;

    fileComments[file] = fileState;
  }
};

const reducers: { [type in DiffActionType]: Reducer } = {
  fetchAll: (state: State, a: DiffAction) => {
    const action = a as FetchAllAction;
    const commentsWithLocations = action.comments.filter(c => !!c.location);

    const files = {};
    const lines = {};
    const comments: { [id: string]: Comment } = {};

    commentsWithLocations.forEach((comment: Comment) => {
      if (isInlineLocation(comment.location) && !comment.outdated) {
        addLineComments(lines, comment);
      } else {
        addFileComments(files, comment);
      }
      comments[comment.id] = comment;
    });

    return {
      files,
      lines,
      comments,
      reviewedFiles: state.reviewedFiles
    };
  },
  createComment: (state: State, a: DiffAction) => {
    const action = a as CommentAction;
    const comment = action.comment;
    if (comment.location) {
      const location = comment.location;
      return produce(state, draft => {
        if (isInlineLocation(location)) {
          addLineComments(draft.lines, comment);
        } else {
          addFileComments(draft.files, comment);
        }
        draft.comments[comment.id] = comment;
      });
    }
  },
  updateComment: (state: State, a: DiffAction) => {
    const action = a as CommentAction;
    const comment = action.comment;
    return produce(state, draft => {
      draft.comments[comment.id] = comment;
    });
  },
  deleteComment: (state: State, a: DiffAction) => {
    const action = a as CommentAction;
    const comment = action.comment;
    if (comment.location) {
      const location = comment.location;
      return produce(state, draft => {
        if (isInlineLocation(location)) {
          removeLineComment(draft.lines, comment);
        } else {
          removeFileComment(draft.files, comment);
        }
        delete draft.comments[comment.id];
      });
    }
  },
  createReply: (state: State, a: DiffAction) => {
    const { parentId, reply } = a as ReplyAction;
    return produce(state, draft => {
      const comment = draft.comments[parentId];
      if (comment) {
        if (!comment._embedded) {
          comment._embedded = {};
        }
        if (!comment._embedded.replies) {
          comment._embedded.replies = [];
        }
        comment._embedded.replies.push(reply);
      }
    });
  },
  deleteReply: (state: State, a: DiffAction) => {
    const { parentId, reply } = a as ReplyAction;
    return produce(state, draft => {
      const comment = draft.comments[parentId];
      if (comment && comment._embedded && comment._embedded.replies) {
        const index = comment._embedded.replies.findIndex((c: Comment) => c.id === reply.id);
        if (index >= 0) {
          comment._embedded.replies.splice(index, 1);
        }
      }
    });
  },
  updateReply: (state: State, a: DiffAction) => {
    const { parentId, reply } = a as ReplyAction;
    return produce(state, draft => {
      const comment = draft.comments[parentId];
      if (comment && comment._embedded && comment._embedded.replies) {
        const index = comment._embedded.replies.findIndex((c: Comment) => c.id === reply.id);
        if (index >= 0) {
          comment._embedded.replies[index] = reply;
        }
      }
    });
  },
  openEditor: (state: State, a: DiffAction) => {
    const { location } = a as EditorAction;
    return produce(state, draft => {
      setEditState(draft, true, location);
    });
  },
  closeEditor: (state: State, a: DiffAction) => {
    const { location } = a as EditorAction;
    return produce(state, draft => {
      setEditState(draft, false, location);
    });
  },
  markAsReviewed: (state: State, a: DiffAction) => {
    const { filepath } = a as ReviewMarkAction;
    return produce(state, draft => {
      draft.reviewedFiles.push(filepath);
    });
  },
  unmarkAsReviewed: (state: State, a: DiffAction) => {
    const { filepath } = a as ReviewMarkAction;
    return produce(state, draft => {
      const index = draft.reviewedFiles.indexOf(filepath);
      if (index >= 0) {
        draft.reviewedFiles.splice(index, 1);
      }
    });
  }
};

const reducer = (state: State, action: DiffAction) => {
  const nextState = reducers[action.type](state, action);
  return nextState || state;
};

export default reducer;
