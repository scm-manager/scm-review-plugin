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
import { AppliedRule, EngineConfiguration } from "../types/EngineConfig";
import { useTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = {
  configuration: EngineConfiguration;
  deleteRule: (rule: AppliedRule) => void;
};

const VCenteredTd = styled.td`
  display: table-cell;
  vertical-align: middle !important;
`;

const NoBorderLeft = styled.table`
  & td:first-child {
    border-left: none;
  }
`;

const EngineConfigTable: FC<Props> = ({ configuration, deleteRule }) => {
  const [t] = useTranslation("plugins");

  return (
    <NoBorderLeft className="card-table table is-hoverable is-fullwidth">
      <thead>
        <tr>
          <th>{t("scm-review-plugin.workflow.rule.column.name")}</th>
          <th>{t("scm-review-plugin.workflow.rule.column.description")}</th>
          <td className="has-no-style" />
        </tr>
      </thead>
      <tbody>
        {configuration.rules?.map(appliedRule => (
          <tr>
            <VCenteredTd>
              <strong>{t(`workflow.rule.${appliedRule.rule}.name`)}</strong>
            </VCenteredTd>
            <VCenteredTd>{t(`workflow.rule.${appliedRule.rule}.description`, appliedRule.configuration)}</VCenteredTd>
            <VCenteredTd>
              <Button
                color="text"
                icon="trash"
                action={() => deleteRule(appliedRule)}
                title={t("scm-review-plugin.workflow.deleteRule")}
                className="px-2"
              />
            </VCenteredTd>
          </tr>
        ))}
      </tbody>
    </NoBorderLeft>
  );
};

export default EngineConfigTable;
