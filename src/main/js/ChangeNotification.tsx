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
import { apiClient, Toast, ToastButtons, ToastButton } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "react-query";
import { createDiffUrl } from "./pullRequest";

type HandlerProps = {
  url: string;
  diffUrl?: string;
  reload: () => void;
};

const EventNotificationHandler: FC<HandlerProps> = ({ url, diffUrl, reload }) => {
  const queryClient = useQueryClient();
  const [event, setEvent] = useState<unknown>();
  useEffect(() => {
    return apiClient.subscribe(url, {
      pullRequest: setEvent
    });
  }, [url]);
  const { t } = useTranslation("plugins");

  const reloadAndClose = async () => {
    if (diffUrl) {
      await queryClient.invalidateQueries(["link", diffUrl]);
    }
    reload();
    setEvent(null);
  };

  if (event) {
    return (
      <Toast type="warning" title={t("scm-review-plugin.changeNotification.title")}>
        <p>{t("scm-review-plugin.changeNotification.description")}</p>
        <p>{t("scm-review-plugin.changeNotification.modificationWarning")}</p>
        <ToastButtons>
          <ToastButton icon="redo" onClick={reloadAndClose}>
            {t("scm-review-plugin.changeNotification.buttons.reload")}
          </ToastButton>
          <ToastButton icon="times" onClick={() => setEvent(undefined)}>
            {t("scm-review-plugin.changeNotification.buttons.ignore")}
          </ToastButton>
        </ToastButtons>
      </Toast>
    );
  }
  return null;
};

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  reload: () => void;
};

const ChangeNotification: FC<Props> = ({ repository, pullRequest, reload }) => {
  if (pullRequest._links.events) {
    const link = pullRequest._links.events as Link;
    let diffLink;
    if (pullRequest.source && pullRequest.target) {
      diffLink = createDiffUrl(repository, pullRequest.source, pullRequest.target);
    }
    return <EventNotificationHandler url={link.href} diffUrl={diffLink} reload={reload} />;
  }
  return null;
};

export default ChangeNotification;
