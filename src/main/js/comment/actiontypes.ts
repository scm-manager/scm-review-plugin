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

import { Comment, Location } from "../types/PullRequest";

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
};

export const openEditor = (location?: Location): EditorAction => {
  return {
    location,
    type: "openEditor"
  };
};

export const closeEditor = (location?: Location): EditorAction => {
  return {
    location,
    type: "closeEditor"
  };
};
