import React, { FC, useEffect, useState } from "react";
import { Link } from "@scm-manager/ui-types";
import { apiClient, Toast, ToastButtons, ToastButton } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { useTranslation } from "react-i18next";

type HandlerProps = {
  url: string;
  reload: () => void;
};

const EventNotificationHandler: FC<HandlerProps> = ({ url, reload }) => {
  const [event, setEvent] = useState<any>();
  useEffect(() => {
    return apiClient.subscribe(url, {
      pullRequest: setEvent
    });
  }, [url]);
  const { t } = useTranslation("plugins");
  if (event) {
    return (
      <Toast type="warning" title={t("scm-review-plugin.changeNotification.title")}>
        <p>{t("scm-review-plugin.changeNotification.description")}</p>
        <p>{t("scm-review-plugin.changeNotification.modificationWarning")}</p>
        <ToastButtons>
          <ToastButton icon="redo" onClick={reload}>
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
  pullRequest: PullRequest;
  reload: () => void;
};

const ChangeNotification: FC<Props> = ({ pullRequest, reload }) => {
  if (pullRequest._links.events) {
    const link = pullRequest._links.events as Link;
    return <EventNotificationHandler url={link.href} reload={reload} />;
  }
  return null;
};

export default ChangeNotification;
