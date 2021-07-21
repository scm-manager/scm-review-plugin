import { FileDiff } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { useMemo, useReducer } from "react";

type State = {
  [key: string]: boolean;
};

type CollapseAll = {
  type: "collapseAll";
};

type UncollapseAll = {
  type: "uncollapseAll";
};

type ToggleCollapse = {
  file: FileDiff;
  type: "toggleCollapse";
};

type MarkAsReviewed = {
  file: FileDiff;
  type: "markAsReviewed";
};

type UnmarkAsReviewed = {
  file: FileDiff;
  type: "unmarkAsReviewed";
};

type OpenFileCommentEditor = {
  file: FileDiff;
  type: "openFileCommentEditor";
};

type Action = CollapseAll | UncollapseAll | ToggleCollapse | MarkAsReviewed | UnmarkAsReviewed | OpenFileCommentEditor;

const createKey = (file: FileDiff) => {
  if (file.newPath !== "/dev/null") {
    return file.newPath;
  } else {
    return file.oldPath;
  }
};

const createInitialState = (reviewMarks: string[]) => {
  const initialState: State = {};

  reviewMarks.forEach(key => {
    initialState[key] = true;
  });
  return initialState;
};

const reducer = (state: State, action: Action) => {
  switch (action.type) {
    case "collapseAll": {
      const newState: State = {};
      Object.keys(state).forEach(key => {
        newState[key] = true;
      });
      return newState;
    }
    case "uncollapseAll": {
      const newState: State = {};
      Object.keys(state).forEach(key => {
        newState[key] = false;
      });
      return newState;
    }
    case "toggleCollapse": {
      const key = createKey(action.file);
      return { ...state, [key]: !state[key] };
    }
    case "markAsReviewed": {
      const key = createKey(action.file);
      return { ...state, [key]: true };
    }
    case "unmarkAsReviewed":
    case "openFileCommentEditor": {
      const key = createKey(action.file);
      return { ...state, [key]: false };
    }

    default: {
      return state;
    }
  }
};

export const useDiffCollapseState = (pullRequest: PullRequest) => {
  const [state, dispatch] = useReducer(reducer, createInitialState(pullRequest.markedAsReviewed || []));

  const actions = useMemo(
    () => ({
      openFileCommentEditor: (file: FileDiff) => {
        dispatch({ type: "openFileCommentEditor", file });
      },
      unmarkAsReviewed: (file: FileDiff) => {
        dispatch({ type: "unmarkAsReviewed", file });
      },
      markAsReviewed: (file: FileDiff) => {
        dispatch({ type: "markAsReviewed", file });
      },
      toggleCollapse: (file: FileDiff) => {
        dispatch({ type: "toggleCollapse", file });
      },
      uncollapseAll: () => {
        dispatch({ type: "uncollapseAll" });
      },
      collapseAll: () => {
        dispatch({ type: "collapseAll" });
      }
    }),
    [dispatch]
  );

  return {
    isCollapsed: (file: FileDiff) => {
      const key = createKey(file);
      const collapsed = state[key];
      if (collapsed === undefined) {
        state[key] = false;
      }
      return state[createKey(file)] || false;
    },
    actions
  };
};
