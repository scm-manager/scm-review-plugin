import React, { FC } from "react";
import { Comment, PossibleTransition } from "../types/PullRequest";
import ToolbarIcon from "./ToolbarIcon";
import Toolbar from "./Toolbar";
import { useTranslation } from "react-i18next";

type Props = {
  comment: Comment;
  collapsed: boolean;
  createLink: string;
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

const CommentActionToolbar: FC<Props> = ({ comment, createLink, collapsed, ...actions }) => {
  const hasLink = (link: string) => {
    return !!comment._links[link];
  };

  const hasTransition = (name: string) => {
    return createLink && containsPossibleTransition(comment, name);
  };

  const icons = [];

  const { t } = useTranslation("plugins");

  if (hasLink("delete")) {
    icons.push(
      <ToolbarIcon
        key="delete"
        title={t("scm-review-plugin.comment.delete")}
        icon={"trash"}
        onClick={actions.onDelete}
      />
    );
  }

  if (hasLink("update")) {
    icons.push(
      <ToolbarIcon
        key="update"
        title={t("scm-review-plugin.comment.update")}
        icon={"edit"}
        onClick={actions.onUpdate}
      />
    );
  }

  if (hasLink("reply")) {
    icons.push(
      <ToolbarIcon key="reply" title={t("scm-review-plugin.comment.reply")} icon={"reply"} onClick={actions.onReply} />
    );
  }

  if (hasTransition("SET_DONE")) {
    icons.push(
      <ToolbarIcon
        key="setDone"
        title={t("scm-review-plugin.comment.done")}
        icon={"check-circle"}
        onClick={() => actions.onTransitionChange("SET_DONE", "scm-review-plugin.comment.confirmDoneAlert")}
      />
    );
  }

  if (hasTransition("MAKE_TASK")) {
    icons.push(
      <ToolbarIcon
        key="makeTask"
        title={t("scm-review-plugin.comment.makeTask")}
        icon={"tasks"}
        onClick={() => actions.onTransitionChange("MAKE_TASK", "scm-review-plugin.comment.confirmMakeTaskAlert")}
      />
    );
  }

  if (hasTransition("REOPEN")) {
    icons.push(
      <ToolbarIcon
        key="reOpen"
        title={t("scm-review-plugin.comment.reopen")}
        icon={"undo"}
        onClick={() => actions.onTransitionChange("REOPEN", "scm-review-plugin.comment.reopenAlert")}
      />
    );
  }

  if (hasTransition("MAKE_COMMENT")) {
    icons.push(
      <ToolbarIcon
        key="makeComment"
        title={t("scm-review-plugin.comment.makeComment")}
        icon={"comment-dots"}
        onClick={() => actions.onTransitionChange("MAKE_COMMENT", "scm-review-plugin.comment.makeCommentAlert")}
      />
    );
  }

  return <Toolbar collapsed={collapsed}>{icons}</Toolbar>;
};

export default CommentActionToolbar;
