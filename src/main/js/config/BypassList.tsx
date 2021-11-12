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
import { ProtectionBypass } from "../types/Config";
import { useTranslation } from "react-i18next";
import { Radio, GroupAutocomplete, UserAutocomplete, Button, Notification, Icon } from "@scm-manager/ui-components";
import { Link, SelectValue } from "@scm-manager/ui-types";
import { useIndexLinks } from "@scm-manager/ui-api";
import styled from "styled-components";

const VCenteredTd = styled.td`
  display: table-cell;
  vertical-align: middle !important;
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
    onChange(
      bypasses.filter(
        bypass => bypass.name !== removedBypass.name || bypass.group !== removedBypass.group
      )
    );
  };

  const table =
    bypasses.length === 0 ? (
      <Notification type={"info"}>
        {t("scm-review-plugin.config.branchProtection.bypasses.noBypasses")}
      </Notification>
    ) : (
      <table className="card-table table is-hoverable is-fullwidth">
        <thead>
          <tr>
            <th>{t("scm-review-plugin.config.branchProtection.bypasses.bypassedUserOrGroup")}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {bypasses.map(bypass => (
            <tr>
              <td>
                <PermissionIcon bypass={bypass} /> {bypass.name}
              </td>
              <VCenteredTd className="is-darker">
                <a
                  className="level-item"
                  onClick={() => deleteBypass(bypass)}
                  title={t("scm-review-plugin.config.branchProtection.bypasses.deleteBypass")}
                >
                  <span className="icon is-small">
                    <Icon name="trash" color="inherit" />
                  </span>
                </a>
              </VCenteredTd>
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
