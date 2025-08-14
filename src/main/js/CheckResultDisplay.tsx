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
import { CheckResult } from "./types/PullRequest";
import styled from "styled-components";
import { useTranslation } from "react-i18next";

type Props = {
  checkResult?: CheckResult;
};

const ValidationError = styled.fieldset`
  font-size: 0.75rem;
`;

const CheckResultDisplay: FC<Props> = ({ checkResult }) => {
  const [t] = useTranslation("plugins");

  if (checkResult && checkResult.status !== "PR_VALID") {
    return (
      <ValidationError className="has-text-danger">
        {t(`scm-review-plugin.pullRequest.validation.${checkResult.status}`)}
      </ValidationError>
    );
  } else {
    return null;
  }
};

export { CheckResultDisplay };
