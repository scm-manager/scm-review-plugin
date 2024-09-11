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
import { ErrorNotification } from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { useSubscription, useUpdateSubscription } from "./pullRequest";
import SubscribeButton from "./SubscribeButton";
import UnsubscribeButton from "./UnsubscribeButton";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const SubscriptionContainer: FC<Props> = ({ repository, pullRequest }) => {
  const { data, error: subscriptionError, isLoading: subscriptionLoading } = useSubscription(repository, pullRequest);
  const { isLoading: updateLoading, subscribe, unsubscribe, error: updateError } = useUpdateSubscription(
    repository,
    pullRequest,
    data || { _links: {} }
  );

  const error = subscriptionError || updateError;
  const loading = subscriptionLoading || updateLoading;

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (data) {
    if (subscribe) {
      return <SubscribeButton loading={loading} action={subscribe} />;
    }
    if (unsubscribe) {
      return <UnsubscribeButton loading={loading} action={unsubscribe} />;
    }
  }
  return null;
};

export default SubscriptionContainer;
