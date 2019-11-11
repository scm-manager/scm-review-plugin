import { Comment } from "../types/PullRequest";
import { Action, ActionType, CommentAction, ReplyAction, FetchAllAction } from "./actiontypes";
import produce from "immer";

export type State = Comment[];
export const initialState: State = [];

type Reducer = (state: State, action: Action) => State | undefined;

const reducers: { [type in ActionType]: Reducer } = {
  fetchAll: (state: State, a: Action) => {
    const action = a as FetchAllAction;
    return action.comments;
  },
  createComment: (state: State, a: Action) => {
    const action = a as CommentAction;
    return produce(state, draft => {
      draft.push(action.comment);
    });
  },
  deleteComment: (state: State, a: Action) => {
    const action = a as CommentAction;
    const index = state.findIndex(c => c.id === action.comment.id);
    if (index >= 0) {
      return produce(state, draft => {
        draft.splice(index, 1);
      });
    }
  },
  updateComment: (state: State, a: Action) => {
    const action = a as CommentAction;
    const index = state.findIndex(c => c.id === action.comment.id);
    if (index >= 0) {
      return produce(state, draft => {
        draft[index] = action.comment;
      });
    }
  },
  createReply: (state: State, a: Action) => {
    const action = a as ReplyAction;
    const index = state.findIndex(c => c.id === action.parentId);
    if (index >= 0) {
      return produce(state, draft => {
        const root = draft[index];
        if (!root._embedded) {
          root._embedded = {};
        }
        if (!root._embedded.replies) {
          root._embedded.replies = [];
        }
        root._embedded.replies.push(action.reply);
      });
    }
  },
  deleteReply: (state: State, a: Action) => {
    const action = a as ReplyAction;
    const index = state.findIndex(c => c.id === action.parentId);
    if (index >= 0) {
      return produce(state, draft => {
        const root = draft[index];
        if (root._embedded && root._embedded.replies) {
          const replyIndex = root._embedded.replies.findIndex((r: Comment) => r.id === action.reply.id);
          if (replyIndex >= 0) {
            root._embedded.replies.splice(replyIndex, 1);
          }
        }
      });
    }
  },
  updateReply: (state: State, a: Action) => {
    const action = a as ReplyAction;
    const index = state.findIndex(c => c.id === action.parentId);
    if (index >= 0) {
      return produce(state, draft => {
        const root = draft[index];
        if (root._embedded && root._embedded.replies) {
          const replyIndex = root._embedded.replies.findIndex((r: Comment) => r.id === action.reply.id);
          root._embedded.replies[replyIndex] = action.reply;
        }
      });
    }
  }
};

const reducer = (state: State, action: Action) => {
  const nextState = reducers[action.type](state, action);
  return nextState || state;
};

export default reducer;
