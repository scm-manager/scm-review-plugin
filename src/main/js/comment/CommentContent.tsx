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

import React, { FC, useMemo } from "react";
import { Comment } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import ReducedMarkdownView from "../ReducedMarkdownView";
import { binder } from "@scm-manager/ui-extensions";
import { AstPlugin } from "@scm-manager/ui-components";

type Props = {
  comment: Comment;
};

const CommentContent: FC<Props> = ({ comment }) => {
  const { t } = useTranslation("plugins");
  const message = useMemo<string>(() => {
    let content = comment.comment!;
    if (comment.systemComment) {
      content = t(`scm-review-plugin.comment.systemMessage.${comment.comment}`, comment.systemCommentParameters);
    }
    comment.mentions.forEach(m => {
      const matcher = new RegExp("@\\[" + m.id + "\\]", "g");
      content = content.replace(matcher, `[${m.displayName}](mailto:${m.mail})`);
    });
    return content;
  }, [t, comment.systemComment, comment.comment, comment.mentions]);
  const astPlugins = useMemo(
    () =>
      binder
        .getExtensions("pullrequest.comment.plugins", {
          halObject: comment
        })
        .map(pluginFactory => pluginFactory({ halObject: comment }) as AstPlugin),
    [comment]
  );

  return <ReducedMarkdownView content={message} plugins={astPlugins} />;
};

export default CommentContent;
