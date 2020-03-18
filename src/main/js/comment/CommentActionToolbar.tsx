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
import React, { FC } from "react";
import { Comment, PossibleTransition } from "../types/PullRequest";
import ToolbarIcon from "./ToolbarIcon";
import Toolbar from "./Toolbar";
import { useTranslation } from "react-i18next";

type Props = {
  parent?: Comment;
  comment: Comment;
  collapsed: boolean;
  createLink?: string;
  onUpdate: () => void;
  onDelete: () => void;
  onReply: () => void;
  onTransitionChange: (transition: string, i18nKey: string) => void;
};

const containsPossibleTransition = (comment: Comment, name: string) => {
  return (
    comment._embedded &&
    comment._embedded.possibleTransitions &&
    comment._embedded.possibleTransitions.find((t: PossibleTransition) => t.name === name)
  );
};

const CommentActionToolbar: FC<Props> = ({ parent, comment, createLink, collapsed, ...actions }) => {
  const hasLink = (link: string) => {
    return !!comment._links[link];
  };

  const hasTransition = (name: string) => {
    return createLink && containsPossibleTransition(comment, name);
  };

  const isLastReply = () => {
    if (parent && parent._embedded && parent._embedded.replies) {
      const replies = parent._embedded && (parent._embedded.replies as Comment[]);
      const lastReply = replies[replies.length - 1];
      return lastReply.id === comment.id;
    }
    return false;
  };

  const isEmptyRootComment = () => {
    return !parent && (!comment._embedded || !comment._embedded.replies || comment._embedded.replies.length === 0);
  };

  const isReplyable = () => {
    if (isEmptyRootComment()) {
      return !!comment._links.reply;
    } else if (parent && isLastReply()) {
      // we have to get the link from the parent, after change
      return !!parent._links.reply;
    } else {
      return false;
    }
  };

  const isDeletable = () => {
    return parent || (isEmptyRootComment() && hasLink("delete"));
  };

  const icons = [];

  const { t } = useTranslation("plugins");

  if (isDeletable()) {
    icons.push(
      <ToolbarIcon key="delete" title={t("scm-review-plugin.comment.delete")} icon="trash" onClick={actions.onDelete} />
    );
  }

  if (hasLink("update")) {
    icons.push(
      <ToolbarIcon key="update" title={t("scm-review-plugin.comment.update")} icon="edit" onClick={actions.onUpdate} />
    );
  }

  if (isReplyable()) {
    icons.push(
      <ToolbarIcon key="reply" title={t("scm-review-plugin.comment.reply")} icon="reply" onClick={actions.onReply} />
    );
  }

  if (hasTransition("SET_DONE")) {
    icons.push(
      <ToolbarIcon
        key="setDone"
        title={t("scm-review-plugin.comment.done")}
        icon="check-circle"
        onClick={() => actions.onTransitionChange("SET_DONE", "scm-review-plugin.comment.confirmDoneAlert")}
      />
    );
  }

  if (hasTransition("MAKE_TASK")) {
    icons.push(
      <ToolbarIcon
        key="makeTask"
        title={t("scm-review-plugin.comment.makeTask")}
        icon="tasks"
        onClick={() => actions.onTransitionChange("MAKE_TASK", "scm-review-plugin.comment.confirmMakeTaskAlert")}
      />
    );
  }

  if (hasTransition("REOPEN")) {
    icons.push(
      <ToolbarIcon
        key="reOpen"
        title={t("scm-review-plugin.comment.reopen")}
        icon="undo"
        onClick={() => actions.onTransitionChange("REOPEN", "scm-review-plugin.comment.reopenAlert")}
      />
    );
  }

  if (hasTransition("MAKE_COMMENT")) {
    icons.push(
      <ToolbarIcon
        key="makeComment"
        title={t("scm-review-plugin.comment.makeComment")}
        icon="comment-dots"
        onClick={() => actions.onTransitionChange("MAKE_COMMENT", "scm-review-plugin.comment.makeCommentAlert")}
      />
    );
  }

  return <Toolbar collapsed={collapsed}>{icons}</Toolbar>;
};

export default CommentActionToolbar;
