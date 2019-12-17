import React, { FC, useEffect, useState } from "react";
import { Repository } from "@scm-manager/ui-types";
import { apiClient, Toast, ToastButtons, ToastButton } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  reload: () => void;
};

const ChangeNotification: FC<Props> = ({ repository, pullRequest, reload }) => {
  const [event, setEvent] = useState();
  useEffect(() => {
    return apiClient.subscribe(`/pull-requests/${repository.namespace}/${repository.name}/${pullRequest.id}/events`, {
      PULL_REQUEST: setEvent,
      COMMENT: setEvent
    });
  }, [repository, pullRequest]);
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

export default ChangeNotification;
