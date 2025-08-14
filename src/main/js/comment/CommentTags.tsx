/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC } from "react";
import { Comment } from "../types/PullRequest";
import { EmergencyMergeTag, FileTag, OutdatedTag, SystemTag, TaskDoneTag, TaskTodoTag } from "./tags";
import { useTranslation } from "react-i18next";
import TagGroup from "./TagGroup";
import { findLatestTransition } from "./transitions";

type Props = {
  comment: Comment;
  onOpenContext?: () => void;
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

  if (comment.emergencyMerged) {
    tags.push(<EmergencyMergeTag />);
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
