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

import React from "react";
import { useTranslation } from "react-i18next";
import { CardColumnSmall, DateFromNow } from "@scm-manager/ui-components";
import { SmallPullRequestIcon } from "./SmallPullRequestIcon";

const PullRequestReopenedEvent = ({ event }) => {
  const [t] = useTranslation("plugins");
  const link = `/repo/${event.namespace}/${event.name}/pull-request/${event.id}`;
  const footer = (
    <>
      {t("scm-review-plugin.landingpage.reopened.footer", {
        author: event.author,
        name: event.name,
        namespace: event.namespace
      })}
    </>
  );

  return (
    <CardColumnSmall
      link={link}
      avatar={<SmallPullRequestIcon />}
      contentLeft={<strong>{t("scm-review-plugin.landingpage.reopened.header", event)}</strong>}
      footer={footer}
      contentRight={
        <small>
          <DateFromNow date={event.date} />
        </small>
      }
    />
  );
};

PullRequestReopenedEvent.type = "PullRequestReopenedEvent";

export default PullRequestReopenedEvent;
