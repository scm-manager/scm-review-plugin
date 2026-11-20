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
import { Level, Notification, Button, Icon, IconButton } from "@scm-manager/ui-core";
import { InputField } from "@scm-manager/ui-components";
import styled from "styled-components";
import { BranchProtection } from "../types/Config";

type Props = {
  protections: BranchProtection[];
  onChange: (protections: BranchProtection[]) => void;
};

const VCenteredTd = styled.td`
  vertical-align: middle !important;
`;

const WidthVCenteredTd = styled(VCenteredTd)`
  width: 5rem;
`;

const FullWidthInputField = styled(InputField)`
  width: 100%;
  margin-right: 1.5rem;
`;

const BranchList: FC<Props> = ({ protections, onChange }) => {
  const [newProtection, setNewProtection] = useState<BranchProtection>({ branch: "", path: "" });
  const [t] = useTranslation("plugins");

  const table =
    protections.length === 0 ? (
      <Notification type={"info"}>{t("scm-review-plugin.config.branchProtection.branches.noBranches")}</Notification>
    ) : (
      <table className="card-table table is-hoverable is-fullwidth">
        <thead>
          <tr>
            <th>{t("scm-review-plugin.config.branchProtection.branches.newBranch.pattern")}</th>
            <th>{t("scm-review-plugin.config.branchProtection.branches.newPath.pattern")}</th>
            <th />
            <td className="has-no-style" />
          </tr>
        </thead>
        <tbody>
          {protections.map(protection => (
            <tr key={protection.branch + protection.path}>
              <VCenteredTd>{protection.branch}</VCenteredTd>
              <VCenteredTd>{protection.path}</VCenteredTd>
              <WidthVCenteredTd className="has-text-centered">
                <IconButton
                  onClick={() =>
                    onChange([...protections.filter(p => p.branch !== protection.branch || p.path !== protection.path)])
                  }
                  title={t("scm-review-plugin.config.branchProtection.branches.deleteBranch")}
                  aria-label={t("scm-review-plugin.config.branchProtection.branches.deleteBranch")}
                >
                  <Icon>trash</Icon>
                </IconButton>
              </WidthVCenteredTd>
            </tr>
          ))}
        </tbody>
      </table>
    );

  return (
    <>
      {table}
      <Level
        className="is-align-items-stretch mb-4"
        right={
          <div className="field is-align-self-flex-end">
            <Button
              title={t("scm-review-plugin.config.branchProtection.branches.add.helpText")}
              onClick={() => {
                onChange([...protections, newProtection]);
                setNewProtection({ branch: "", path: "" });
              }}
              disabled={newProtection.branch.trim().length === 0 || newProtection.path.trim().length === 0}
            >
              <Icon>plus</Icon>
              <span>{t("scm-review-plugin.config.branchProtection.branches.add.label")}</span>
            </Button>
          </div>
        }
      >
        <FullWidthInputField
          label={t("scm-review-plugin.config.branchProtection.branches.newBranch.label")}
          onChange={branch => setNewProtection(prevState => ({ ...prevState, branch }))}
          value={newProtection.branch}
        />
        <FullWidthInputField
          label={t("scm-review-plugin.config.branchProtection.branches.newPath.label")}
          onChange={path => setNewProtection(prevState => ({ ...prevState, path }))}
          value={newProtection.path}
        />
      </Level>
    </>
  );
};

export default BranchList;
