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
