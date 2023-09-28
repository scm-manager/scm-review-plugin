/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { AddButton, Button, InputField, Level, Notification } from "@scm-manager/ui-components";
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
                <Button
                  color="text"
                  icon="trash"
                  action={() =>
                    onChange([...protections.filter(p => p.branch !== protection.branch && p.path !== protection.path)])
                  }
                  title={t("scm-review-plugin.config.branchProtection.branches.deleteBranch")}
                  className="px-2"
                />
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
        children={
          <>
            <FullWidthInputField
              label={t("scm-review-plugin.config.branchProtection.branches.newBranch.label")}
              onChange={branch => setNewProtection(prevState => ({ ...prevState, branch }))}
              value={newProtection.branch}
              helpText={t("scm-review-plugin.config.branchProtection.branches.newBranch.helpText")}
            />
            <FullWidthInputField
              label={t("scm-review-plugin.config.branchProtection.branches.newPath.label")}
              onChange={path => setNewProtection(prevState => ({ ...prevState, path }))}
              value={newProtection.path}
              helpText={t("scm-review-plugin.config.branchProtection.branches.newPath.helpText")}
            />
          </>
        }
        right={
          <div className="field is-align-self-flex-end">
            <AddButton
              title={t("scm-review-plugin.config.branchProtection.branches.add.helpText")}
              label={t("scm-review-plugin.config.branchProtection.branches.add.label")}
              action={() => {
                onChange([...protections, newProtection]);
                setNewProtection({ branch: "", path: "" });
              }}
              disabled={newProtection.branch.trim().length === 0 || newProtection.path.trim().length === 0}
            />
          </div>
        }
      />
    </>
  );
};

export default BranchList;
