import {Comment, Location} from "../types/PullRequest";

export type ActionType =
  | "fetchAll"
  | "createComment"
  | "updateComment"
  | "deleteComment"
  | "createReply"
  | "updateReply"
  | "deleteReply"
  | "openEditor"
  | "closeEditor";

export type Action = {
  type: ActionType;
};

export type FetchAllAction = Action & {
  comments: Comment[];
};

export const fetchAll = (comments: Comment[]): FetchAllAction => {
  return {
    comments,
    type: "fetchAll"
  };
};

export type CommentAction = Action & {
  comment: Comment;
};

export const createComment = (comment: Comment): CommentAction => {
  return {
    comment,
    type: "createComment"
  };
};

export const updateComment = (comment: Comment): CommentAction => {
  return {
    comment,
    type: "updateComment"
  };
};

export const deleteComment = (comment: Comment): CommentAction => {
  return {
    comment,
    type: "deleteComment"
  };
};

export type ReplyAction = Action & {
  parentId: string;
  reply: Comment;
};

export const createReply = (parentId: string, reply: Comment): ReplyAction => {
  return {
    parentId,
    reply,
    type: "createReply"
  };
};

export const updateReply = (parentId: string, reply: Comment): ReplyAction => {
  return {
    parentId,
    reply,
    type: "updateReply"
  };
};

export const deleteReply = (parentId: string, reply: Comment): ReplyAction => {
  return {
    parentId,
    reply,
    type: "deleteReply"
  };
};

export type EditorAction = Action & {
  location?: Location;
}

export const openEditor = (location?: Location): EditorAction => {
  return {
    location,
    type: "openEditor"
  }
};

export const closeEditor = (location?: Location): EditorAction => {
  return {
    location,
    type: "closeEditor"
  }
};
