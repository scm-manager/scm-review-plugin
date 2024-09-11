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
import { Button } from "@scm-manager/ui-components";

type Props = {
  loading: boolean;
  action: () => void;
};

const ApprovalButton: FC<Props> = ({ loading, action }) => {
  const [t] = useTranslation("plugins");
  return (
    <Button
      label={t("scm-review-plugin.pullRequest.details.buttons.approve")}
      loading={loading}
      action={action}
      color="link is-outlined"
      icon="check"
    />
  );
};

export default ApprovalButton;
