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
      content = t(`scm-review-plugin.comment.systemMessage.${comment.comment}`);
    }
    comment.mentions.forEach(m => {
      const matcher = new RegExp("@\\[" + m.id + "\\]", "g");
      content = content.replace(matcher, `[${m.displayName}](mailto:${m.mail})`);
    });
    return content;
  }, [t, comment.systemComment, comment.comment, comment.mentions]);

  return (
    <ReducedMarkdownView
      content={message}
      plugins={binder
        .getExtensions("pullrequest.comment.plugins", {
          halObject: comment
        })
        .map(pluginFactory => pluginFactory({ halObject: comment }) as AstPlugin)}
    />
  );
};

export default CommentContent;
