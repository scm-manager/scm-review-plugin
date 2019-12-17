import React, { FC, useEffect, useState } from "react";
import { Link } from "@scm-manager/ui-types";
import { apiClient, Toast, ToastButtons, ToastButton } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";

type HandlerProps = {
  url: string;
  reload: () => void;
};

const EventNotificationHandler: FC<HandlerProps> = ({ url, reload }) => {
  const [event, setEvent] = useState();
  useEffect(() => {
    return apiClient.subscribe(url, {
      pullRequest: setEvent
    });
  }, [url]);
  if (event) {
    return (
      <Toast type="warning" title="New Changes">
        <p>The underlying Pull-Request has changed. Press reload to see the changes.</p>
        <p>Warning: Non saved modification will be lost.</p>
        <ToastButtons>
          <ToastButton icon="redo" onClick={reload}>Reload</ToastButton>
          <ToastButton icon="times" onClick={() => setEvent(undefined)}>Ignore</ToastButton>
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
