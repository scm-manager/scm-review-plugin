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
import { Configuration } from "@scm-manager/ui-components";
import ConfigEditor from "./ConfigEditor";
import { useRepositoryFromParams } from "@scm-manager/ui-core";
import { Link } from "@scm-manager/ui-types";

const RepositoryConfig: FC = () => {
  const repository = useRepositoryFromParams();
  const link = (repository._links["pullRequestConfig"] as Link).href;
  return (
    <Configuration
      link={link}
      render={(props) => {
        return <ConfigEditor {...props} configType="repository" />;
      }}
    />
  );
};

export default RepositoryConfig;
