import {Comment, Location} from "../types/PullRequest";
import { Action, ActionType, CommentAction, ReplyAction, FetchAllAction } from "../comment/actiontypes";
import produce from "immer";
import {createChangeIdFromLocation, createHunkIdFromLocation, isInlineLocation} from "./locations";

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

export type State = DiffRelatedCommentCollection;

export type DiffRelatedCommentCollection = {
  files: FileCommentCollection;
  lines: LineCommentCollection;
  comments: {[key: string]: Comment}
};

export const initialState: State = {
  lines: {

  },
  files: {

  },
  comments: {

  }
};

type Reducer = (state: State, action: Action) => State | undefined;

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

const reducers: { [type in ActionType]: Reducer } = {
  fetchAll: (state: State, a: Action) => {
    const action = a as FetchAllAction;
    const commentsWithLocations = action.comments.filter(c => !!c.location);

    const files = {};
    const lines = {};
    const comments: {[id: string]: Comment} = {};

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
      comments
    };
  },
  createComment: (state: State, a: Action) => {
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
  updateComment: (state: State, a: Action) => {
    const action = a as CommentAction;
    const comment = action.comment;
    return produce(state, draft => {
      draft.comments[comment.id] = comment;
    });
  },
  deleteComment: (state: State, a: Action) => {
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
  createReply: (state: State, a: Action) => {
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
  deleteReply: (state: State, a: Action) => {
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
  updateReply: (state: State, a: Action) => {
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
  }
};

const reducer = (state: State, action: Action) => {
  const nextState = reducers[action.type](state, action);
  return nextState || state;
};

export default reducer;
