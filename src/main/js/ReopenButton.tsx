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

import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { Dialog } from "@scm-manager/ui-overlays";
import { Button } from "@scm-manager/ui-buttons";

type Props = {
  reopen: () => Promise<unknown>;
  loading: boolean;
};

const ReopenButton: FC<Props> = ({ reopen, loading }) => {
  const [t] = useTranslation("plugins");
  const [open, setOpen] = useState<boolean>();

  return (
    <Dialog
      open={open}
      onOpenChange={setOpen}
      title={t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.title")}
      description={t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.message.hint")}
      footer={[
        <Button key="submit" variant="primary" onClick={() => reopen().finally(() => setOpen(false))}>
          {t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.submit")}
        </Button>,
        <Dialog.CloseButton key="cancel" autoFocus>
          {t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.cancel")}
        </Dialog.CloseButton>
      ]}
      trigger={
        <Button isLoading={loading} color="warning">
          {t("scm-review-plugin.showPullRequest.reopenButton.buttonTitle")}
        </Button>
      }
    />
  );
};

export default ReopenButton;
