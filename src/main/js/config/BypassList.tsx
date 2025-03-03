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
import { Button, GroupAutocomplete, Icon, Notification, Radio, UserAutocomplete } from "@scm-manager/ui-components";
import { Link, SelectValue } from "@scm-manager/ui-types";
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
    groups: links?.find(l => l.name === "groups"),
    users: links?.find(l => l.name === "users")
  };
};

const PermissionIcon: FC<{ bypass: ProtectionBypass }> = ({ bypass }) => {
  const [t] = useTranslation("plugins");
  if (bypass.group) {
    return <Icon title={t("scm-review-plugin.config.branchProtection.bypasses.groupBypass")} name="user-friends" />;
  } else {
    return <Icon title={t("scm-review-plugin.config.branchProtection.bypasses.userBypass")} name="user" />;
  }
};

const BypassList: FC<{
  bypasses: ProtectionBypass[];
  onChange: (newBypass: ProtectionBypass[]) => void;
  readOnly?: boolean;
}> = ({ bypasses, onChange, readOnly = false }) => {
  const [t] = useTranslation("plugins");
  const [bypassToAddIsGroup, setBypassToAddIsGroup] = useState(false);
  const [nameToAdd, setNameToAdd] = useState("");
  const [selectedAutocompleteValue, setSelectedAutocompleteValue] = useState<SelectValue>();
  const links = useAutoCompleteLinks();

  const changeToUserBypass = (value: boolean) => {
    if (value) {
      setBypassToAddIsGroup(false);
      setNameToAdd("");
      setSelectedAutocompleteValue(undefined);
    }
  };

  const changeToGroupBypass = (value: boolean) => {
    if (value) {
      setBypassToAddIsGroup(true);
      setNameToAdd("");
      setSelectedAutocompleteValue(undefined);
    }
  };

  const selectName = (selection: SelectValue) => {
    setNameToAdd(selection.value.id);
    setSelectedAutocompleteValue(selection);
  };

  const renderAutocomplete = () => {
    if (bypassToAddIsGroup) {
      return (
        <GroupAutocomplete
          autocompleteLink={links.groups?.href}
          valueSelected={selectName}
          value={selectedAutocompleteValue}
        />
      );
    }
    return (
      <UserAutocomplete
        autocompleteLink={links.users?.href}
        valueSelected={selectName}
        value={selectedAutocompleteValue}
      />
    );
  };

  const addBypass = () => {
    onChange([...bypasses, { name: nameToAdd, group: bypassToAddIsGroup }]);
  };

  const deleteBypass = (removedBypass: ProtectionBypass) => {
    onChange(bypasses.filter(bypass => bypass.name !== removedBypass.name || bypass.group !== removedBypass.group));
  };

  const table =
    bypasses.length === 0 ? (
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
          {bypasses.map(bypass => (
            <tr>
              <VCenteredTd>
                <PermissionIcon bypass={bypass} /> {bypass.name}
              </VCenteredTd>
              <WidthVCenteredTd className="has-text-centered">
                <Button
                  color="text"
                  icon="trash"
                  action={() => deleteBypass(bypass)}
                  title={t("scm-review-plugin.config.branchProtection.bypasses.deleteBypass")}
                  className="px-2"
                />
              </WidthVCenteredTd>
            </tr>
          ))}
        </tbody>
      </table>
    );

  const addDialog = (
    <div className="columns">
      <div className="column is-narrow">
        <label className="label">{t("scm-review-plugin.config.branchProtection.bypasses.bypassType")}</label>
        <div className="field is-grouped">
          <div className="control">
            <Radio
              label={t("scm-review-plugin.config.branchProtection.bypasses.userBypass")}
              name="bypass_scope"
              value="USER_BYPASS"
              checked={!bypassToAddIsGroup}
              onChange={changeToUserBypass}
            />
            <Radio
              label={t("scm-review-plugin.config.branchProtection.bypasses.groupBypass")}
              name="bypass_scope"
              value="GROUP_BYPASS"
              checked={bypassToAddIsGroup}
              onChange={changeToGroupBypass}
            />
          </div>
        </div>
      </div>
      <div className="column">{renderAutocomplete()}</div>
      <div className="column is-narrow">
        <Button
          title={t("scm-review-plugin.config.branchProtection.bypasses.add.helpText")}
          label={t("scm-review-plugin.config.branchProtection.bypasses.add.label")}
          disabled={readOnly || !nameToAdd}
          icon="plus"
          action={addBypass}
          className="label-icon-spacing"
        />
      </div>
    </div>
  );

  return (
    <>
      {table}
      {addDialog}
    </>
  );
};

export default BypassList;
