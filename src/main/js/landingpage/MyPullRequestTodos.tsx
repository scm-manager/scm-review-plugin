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
import { Tag } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { CardColumnSmall, DateFromNow } from "@scm-manager/ui-components";

type Props = {
  task: any;
};
const PullRequestTodos: FC<Props> = ({ task }) => {
  const [t] = useTranslation("plugins");
  const { pullRequest } = task;
  const link = `/repo/${task.namespace}/${task.name}/pull-request/${task.pullRequest.id}`;
  const content = (
    <>
      <Tag
        color={"info"}
        label={t("scm-review-plugin.landingpage.todo.todo", {
          open: pullRequest.tasks.done,
          count: pullRequest.tasks.todo + pullRequest.tasks.done
        })}
        title={t("scm-review-plugin.landingpage.todo.todoTitle", {
          open: pullRequest.tasks.done,
          count: pullRequest.tasks.todo + pullRequest.tasks.done
        })}
      />{" "}
      {t("scm-review-plugin.landingpage.todo.title", pullRequest)}
    </>
  );
  const footer = (
    <>
      {t("scm-review-plugin.landingpage.todo.footer")} <span className="has-text-info">{task.namespace + "/" + task.name}</span>
    </>
  );

  return (
    <CardColumnSmall
      link={link}
      icon={<i className="fas fa-code-branch fa-rotate-180 fa-2x media-left" />}
      contentLeft={content}
      footer={footer}
      contentRight={<DateFromNow date={pullRequest.creationDate} />}
    />
  );
};

PullRequestTodos.type = "MyPullRequestTodos";

export default PullRequestTodos;
