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
import { useApproveReviewer } from "./pullRequest";
import ApprovalButton from "./ApprovalButton";
import DisapprovalButton from "./DisapprovalButton";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

const ApprovalContainer: FC<Props> = ({ repository, pullRequest }) => {
  const { error, isLoading, approve, disapprove } = useApproveReviewer(repository, pullRequest);

  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (approve) {
    return <ApprovalButton loading={isLoading} action={approve} />;
  } else if (disapprove) {
    return <DisapprovalButton loading={isLoading} action={disapprove} />;
  } else {
    return null;
  }
};

export default ApprovalContainer;
