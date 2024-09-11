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
import { useTranslation, withTranslation, WithTranslation } from "react-i18next";
import { Icon } from "@scm-manager/ui-components";
import { Result } from "./types/EngineConfig";

type Props = WithTranslation & {
  result: Result;
};

const OverrideModalRow: FC<Props> = ({ result }) => {
  const [t] = useTranslation("plugins");

  return (
    <div className="is-flex is-align-items-center py-1">
      <Icon className="pr-2" color="warning" name="exclamation-triangle" />
      <p>{t(result?.rule)}</p>
    </div>
  );
};

export default withTranslation("plugins")(OverrideModalRow);
