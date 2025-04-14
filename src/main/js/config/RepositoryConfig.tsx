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

import React from "react";
import { Configuration } from "@scm-manager/ui-components";
import ConfigEditor from "./ConfigEditor";
import { useRepositoryContext } from "@scm-manager/ui-api";
import { useDocumentTitleForRepository } from "@scm-manager/ui-core";
import { useTranslation } from "react-i18next";

type Props = {
  link: string;
};

export default function RepositoryConfig({ link }: Readonly<Props>) {
  const repository = useRepositoryContext();
  const [t] = useTranslation("plugins");

  // @ts-ignore - it's a dedicated repository config
  useDocumentTitleForRepository(repository, t("scm-review-plugin.config.title"));

  return <Configuration link={link} render={(props) => <ConfigEditor {...props} configType="repository" />} />;
}
