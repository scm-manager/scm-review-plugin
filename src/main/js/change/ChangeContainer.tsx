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
import { DEFAULT_PROPERTIES, PullRequestChange } from "./ChangesTypes";
import { useTranslation } from "react-i18next";
import { evalChangeType } from "./util";
import ChangeRenderer from "./ChangeRenderer";
import { DateFromNow } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import ChangeAuthor from "./ChangeAuthor";

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
            <ChangeAuthor person={{ name: change.displayName ?? change.username, mail: change.mail }} />
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
