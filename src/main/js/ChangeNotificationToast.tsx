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
import { Toast, ToastButton, ToastButtons } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";

type Props = {
  reload: () => void;
  ignore: () => void;
};

const ChangeNotificationToast: FC<Props> = ({ reload, ignore }) => {
  const { t } = useTranslation("plugins");

  return (
    <Toast type="warning" title={t("scm-review-plugin.changeNotification.title")}>
      <p>{t("scm-review-plugin.changeNotification.description")}</p>
      <p>{t("scm-review-plugin.changeNotification.modificationWarning")}</p>
      <ToastButtons>
        <ToastButton icon="redo" onClick={reload}>
          {t("scm-review-plugin.changeNotification.buttons.reload")}
        </ToastButton>
        <ToastButton icon="times" onClick={ignore}>
          {t("scm-review-plugin.changeNotification.buttons.ignore")}
        </ToastButton>
      </ToastButtons>
    </Toast>
  );
};

export default ChangeNotificationToast;
