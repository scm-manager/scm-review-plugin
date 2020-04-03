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
import { useTranslation } from "react-i18next";
import MyEventEntry from "./MyEventEntry";

type Props = {
  task: any;
};
const PullRequestReview: FC<Props> = ({ task }) => {
  const [t] = useTranslation("plugins");
  const { pullRequest } = task;
  const link = `/repo/${task.namespace}/${task.name}/pull-request/${task.pullRequest.id}`;
  const content = t("scm-review-plugin.landingpage.review.title", pullRequest);
  const footer = (
    <>
      {t("scm-review-plugin.landingpage.review.footer")}{" "}
      <span className="has-text-info">{task.namespace + "/" + task.name}</span>
    </>
  );

  return (
    <MyEventEntry
      link={link}
      icon={<i className="fas fa-code-branch fa-rotate-180 fa-2x media-left" />}
      header={content}
      footer={footer}
      date={pullRequest.creationDate}
    />
  );
};

PullRequestReview.type = "MyPullRequestReview";

export default PullRequestReview;
