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
import { ProtectionBypass } from "../types/Config";
import { useTranslation } from "react-i18next";
import { Icon, Notification, Tooltip, IconButton, RadioGroupField } from "@scm-manager/ui-core";
import { Autocomplete } from "@scm-manager/ui-components";
import { AutocompleteObject, Link, Option } from "@scm-manager/ui-types";
import { useIndexLinks } from "@scm-manager/ui-api";
import styled from "styled-components";

const VCenteredTd = styled.td`
  vertical-align: middle !important;
`;

const WidthVCenteredTd = styled(VCenteredTd)`
  width: 5rem;
`;

const useAutoCompleteLinks = () => {
  const links = useIndexLinks()?.autocomplete as Link[];
  return {
    groups: links?.find((l) => l.name === "groups"),
    users: links?.find((l) => l.name === "users"),
  };
};

const PermissionIcon: FC<{ bypass: ProtectionBypass }> = ({ bypass }) => {
  const [t] = useTranslation("plugins");
  const label = bypass.group
    ? t("scm-review-plugin.config.branchProtection.bypasses.groupBypass")
    : t("scm-review-plugin.config.branchProtection.bypasses.userBypass");
  return (
    <Tooltip message={label}>
      <Icon role="img" aria-hidden={false} aria-label={label}>
        {bypass.group ? "user-friends" : "user"}
      </Icon>
    </Tooltip>
  );
};

const BypassList: FC<{
  bypasses: ProtectionBypass[];
  onChange: (newBypass: ProtectionBypass[]) => void;
  readOnly?: boolean;
}> = ({ bypasses, onChange, readOnly = false }) => {
  const [t] = useTranslation("plugins");
  const [bypassToAddIsGroup, setBypassToAddIsGroup] = useState(false);
  const links = useAutoCompleteLinks();

  const addBypass = (option: Option<AutocompleteObject>) => {
    const name = option.value.id;
    if (!bypasses.some((b) => b.name === name && b.group === bypassToAddIsGroup)) {
      onChange([...bypasses, { name, group: bypassToAddIsGroup }]);
    }
  };

  const deleteBypass = (removedBypass: ProtectionBypass) => {
    onChange(bypasses.filter((b) => b !== removedBypass));
  };

  return (
    <>
      {bypasses.length === 0 ? (
        <Notification type={"info"}>{t("scm-review-plugin.config.branchProtection.bypasses.noBypasses")}</Notification>
      ) : (
        <table className="card-table table is-hoverable is-fullwidth">
          <thead>
            <tr>
              <th>{t("scm-review-plugin.config.branchProtection.bypasses.bypassedUserOrGroup")}</th>
              <td className="has-no-style" />
            </tr>
          </thead>
          <tbody>
            {bypasses.map((bypass) => (
              <tr>
                <VCenteredTd>
                  <PermissionIcon bypass={bypass} /> {bypass.name}
                </VCenteredTd>
                <WidthVCenteredTd className="has-text-centered">
                  <IconButton
                    onClick={() => deleteBypass(bypass)}
                    title={t("scm-review-plugin.config.branchProtection.bypasses.deleteBypass")}
                    aria-label={t("scm-review-plugin.config.branchProtection.bypasses.deleteBypass")}
                  >
                    <Icon>trash</Icon>
                  </IconButton>
                </WidthVCenteredTd>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {!readOnly && (
        <div>
          <div>
            <RadioGroupField
              label={t("scm-review-plugin.config.branchProtection.bypasses.bypassType")}
              value={bypassToAddIsGroup ? "group" : "user"}
              onValueChange={(value) => setBypassToAddIsGroup(value === "group")}
            >
              <RadioGroupField.Option
                value="user"
                label={t("scm-review-plugin.config.branchProtection.bypasses.userBypass")}
              />
              <RadioGroupField.Option
                value="group"
                label={t("scm-review-plugin.config.branchProtection.bypasses.groupBypass")}
              />
            </RadioGroupField>
          </div>
          <div>
            {(bypassToAddIsGroup ? links.groups : links.users) && (
              <Autocomplete
                type={bypassToAddIsGroup ? "group" : "user"}
                autocompleteLink={bypassToAddIsGroup ? links.groups!.href : links.users!.href}
                onSelect={addBypass}
              />
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default BypassList;
