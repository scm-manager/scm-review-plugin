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
