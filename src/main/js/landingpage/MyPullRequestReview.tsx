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
import { useTranslation } from "react-i18next";
import { CardColumnSmall, DateFromNow } from "@scm-manager/ui-components";
import { DataType } from "./DataType";
import { SmallPullRequestIcon } from "./SmallPullRequestIcon";

type Props = {
  task: DataType;
};
const PullRequestReview: FC<Props> = ({ task }) => {
  const [t] = useTranslation("plugins");
  const { pullRequest } = task;
  const link = `/repo/${task.namespace}/${task.name}/pull-request/${task.pullRequest.id}`;
  const content = t("scm-review-plugin.landingpage.review.title", pullRequest);
  const footer = (
    <>
      {t("scm-review-plugin.landingpage.review.footer")} {task.namespace + "/" + task.name}
    </>
  );

  return (
    <CardColumnSmall
      link={link}
      avatar={<SmallPullRequestIcon />}
      contentLeft={content}
      footer={footer}
      contentRight={
        <small>
          <DateFromNow date={pullRequest.creationDate} />
        </small>
      }
    />
  );
};

export default Object.assign(PullRequestReview, { type: "MyPullRequestReview" });
