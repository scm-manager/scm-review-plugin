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

import React, { FC, useEffect, useState } from "react";
import { Link, Repository } from "@scm-manager/ui-types";
import { apiClient } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { useInvalidatePullRequest } from "./pullRequest";
import ChangeNotificationToast from "./ChangeNotificationToast";
import { useChangeNotificationContext } from "./ChangeNotificationContext";

type HandlerProps = {
  url: string;
  invalidatePullRequest: () => Promise<void[]>;
};

const useSubscription = (url: string) => {
  const [event, setEvent] = useState<unknown>();
  const [visible, setVisible] = useState(document.hasFocus());

  const onFocus = () => {
    // clear previous toast, because react-query refetches anyways
    setEvent(null);
    setVisible(true);
  };

  const onBlur = () => setVisible(false);

  useEffect(() => {
    window.addEventListener("focus", onFocus);
    window.addEventListener("blur", onBlur);
    return () => {
      window.removeEventListener("blur", onBlur);
      document.removeEventListener("focus", onFocus);
    };
  }, []);

  useEffect(() => {
    // we only want to show toasts, if our tab is currently selected
    // this avoids to many connection problems with sse and notifications for already loaded content
    if (visible) {
      return apiClient.subscribe(url, {
        pullRequest: setEvent
      });
    }
  }, [url, visible]);

  return {
    event,
    clear: () => setEvent(null)
  };
};

const EventNotificationHandler: FC<HandlerProps> = ({ url, invalidatePullRequest }) => {
  const { event, clear } = useSubscription(url);
  const ctx = useChangeNotificationContext();

  const reloadAndClose = async () => {
    await invalidatePullRequest();
    // notify other listeners such as diff to avoid duplicated notifications
    ctx.reload();
    clear();
  };

  if (event) {
    return <ChangeNotificationToast reload={reloadAndClose} ignore={clear} />;
  }
  return null;
};

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const ChangeNotification: FC<Props> = ({ repository, pullRequest }) => {
  const invalidatePullRequest = useInvalidatePullRequest(repository, pullRequest, true);
  if (pullRequest._links.events) {
    const link = pullRequest._links.events as Link;
    return <EventNotificationHandler url={link.href} invalidatePullRequest={invalidatePullRequest} />;
  }
  return null;
};

export default ChangeNotification;
