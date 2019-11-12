import React, { FC } from "react";
import { Comment } from "../types/PullRequest";
import { FileTag, OutdatedTag, SystemTag, TaskDoneTag, TaskTodoTag } from "./tags";
import { useTranslation } from "react-i18next";
import TagGroup from "./TagGroup";
import { findLatestTransition } from "./transitions";

type Props = {
  comment: Comment;
  onOpenContext: () => void;
};

const CommentTags: FC<Props> = ({ comment, onOpenContext }) => {
  const { t } = useTranslation("plugins");

  const tags = [];

  if (comment.outdated) {
    tags.push(<OutdatedTag key="outdated" onClick={onOpenContext} />);
  }

  if (comment.location && comment.location.file) {
    tags.push(<FileTag key="file" path={comment.location.file} onClick={onOpenContext} />);
  }

  if (comment.systemComment) {
    tags.push(<SystemTag key="system" />);
  }

  if (comment.type === "TASK_TODO") {
    tags.push(<TaskTodoTag key="todo" />);
  } else if (comment.type === "TASK_DONE") {
    const transition = findLatestTransition(comment, "SET_DONE");
    if (transition) {
      tags.push(
        <TaskDoneTag
          key="done"
          title={t("scm-review-plugin.comment.markedDoneBy") + " " + transition.user.displayName}
        />
      );
    }
  }

  if (tags.length > 0) {
    return <TagGroup>{tags}</TagGroup>;
  }
  return null;
};

export default CommentTags;
