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
import { Icon } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";
import { copyToClipboard, NoStyleButton } from "@scm-manager/ui-components";

type Props = {
  text: string;
};

const CopyToClipboardButton: FC<Props> = ({ text }) => {
  const [t] = useTranslation("plugins");
  const [copied, setCopied] = useState(false);
  const copy = () => {
    copyToClipboard(text).then(() => setCopied(true));
  };

  return (
    <NoStyleButton onClick={() => copy()} title={t("scm-review-plugin.pullRequest.details.copyButton")}>
      <Icon>{copied ? "clipboard-check" : "clipboard"}</Icon>
    </NoStyleButton>
  );
};

export default CopyToClipboardButton;
