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
import { DEFAULT_PROPERTIES, PullRequestChange } from "./ChangesTypes";
import { useTranslation } from "react-i18next";
import { evalChangeType } from "./util";
import ChangeRenderer from "./ChangeRenderer";
import { DateFromNow, SingleContributor } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  change: PullRequestChange;
  repository: Repository;
};

type WithChange = Pick<Props, "change">;

const TitleContent: FC<WithChange> = ({ change }) => {
  const { t } = useTranslation("plugins");
  const changeType = evalChangeType(change);

  if (change.property === DEFAULT_PROPERTIES.COMMENT_TYPE && change.previousValue === "COMMENT") {
    return t("scm-review-plugin.pullRequest.changes.properties.COMMENT_TO_TASK");
  }

  if (change.property === DEFAULT_PROPERTIES.COMMENT_TYPE && change.currentValue === "COMMENT") {
    return t("scm-review-plugin.pullRequest.changes.properties.TASK_TO_COMMENT");
  }

  if (
    change.property === DEFAULT_PROPERTIES.COMMENT_TYPE &&
    change.currentValue === "TASK_TODO" &&
    change.previousValue === "TASK_DONE"
  ) {
    return t("scm-review-plugin.pullRequest.changes.properties.UNFINISHED_TASK");
  }

  if (
    change.property === DEFAULT_PROPERTIES.COMMENT_TYPE &&
    change.currentValue === "TASK_DONE" &&
    change.previousValue === "TASK_TODO"
  ) {
    return t("scm-review-plugin.pullRequest.changes.properties.FINISHED_TASK");
  }

  return t(`scm-review-plugin.pullRequest.changes.titles.${changeType}`, {
    propertyName: t(`scm-review-plugin.pullRequest.changes.properties.${change.property}`)
  });
};

const ChangeTitle: FC<WithChange> = ({ change }) => {
  return (
    <h4 className="panel-block has-text-weight-bold">
      <TitleContent change={change} />
    </h4>
  );
};

const ChangeContainer: FC<Props> = ({ change, repository }) => {
  const { t } = useTranslation("plugins");
  const changeType = evalChangeType(change);

  return (
    <div className="panel mb-4">
      <ChangeTitle change={change} />
      <p className="panel-block">
        {change.username ? (
          <>
            {t("scm-review-plugin.pullRequest.changes.changedBy")}{" "}
            <SingleContributor person={{ name: change.displayName || change.username, mail: change.mail }} />
          </>
        ) : (
          t("scm-review-plugin.pullRequest.changes.changedByUnknown")
        )}
      </p>
      <p className="panel-block">
        {t("scm-review-plugin.pullRequest.changes.changedAt")} <DateFromNow date={change.changedAt} />
      </p>
      <div className="panel-block">
        <ChangeRenderer changeType={changeType} change={change} repository={repository} />
      </div>
    </div>
  );
};

export default ChangeContainer;
