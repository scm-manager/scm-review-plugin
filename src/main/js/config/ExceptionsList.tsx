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
import { ExceptionEntry } from "../types/Config";
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

const PermissionIcon: FC<{ exception: ExceptionEntry }> = ({ exception }) => {
  const [t] = useTranslation("plugins");
  if (exception.group) {
    return (
      <Icon title={t("scm-review-plugin.config.branchProtection.exceptions.groupException")} name="user-friends" />
    );
  } else {
    return <Icon title={t("scm-review-plugin.config.branchProtection.exceptions.userException")} name="user" />;
  }
};

const ExceptionsList: FC<{
  exceptions: ExceptionEntry[];
  onChange: (newExceptions: ExceptionEntry[]) => void;
  readOnly?: boolean;
}> = ({ exceptions, onChange, readOnly = false }) => {
  const [t] = useTranslation("plugins");
  const [exceptionToAddIsGroup, setExceptionToAddIsGroup] = useState(false);
  const [nameToAdd, setNameToAdd] = useState("");
  const [selectedAutocompleteValue, setSelectedAutocompleteValue] = useState<SelectValue>();
  const links = useAutoCompleteLinks();

  const changeToUserException = (value: boolean) => {
    if (value) {
      setExceptionToAddIsGroup(false);
      setNameToAdd("");
      setSelectedAutocompleteValue(undefined);
    }
  };

  const changeToGroupException = (value: boolean) => {
    if (value) {
      setExceptionToAddIsGroup(true);
      setNameToAdd("");
      setSelectedAutocompleteValue(undefined);
    }
  };

  const selectName = (selection: SelectValue) => {
    setNameToAdd(selection.value.id);
    setSelectedAutocompleteValue(selection);
  };

  const renderAutocomplete = () => {
    if (exceptionToAddIsGroup) {
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

  const addException = () => {
    onChange([...exceptions, { name: nameToAdd, group: exceptionToAddIsGroup }]);
  };

  const deleteException = (removedException: ExceptionEntry) => {
    onChange(
      exceptions.filter(
        exception => exception.name !== removedException.name || exception.group !== removedException.group
      )
    );
  };

  const table =
    exceptions.length === 0 ? (
      <Notification type={"info"}>
        {t("scm-review-plugin.config.branchProtection.exceptions.noExceptions")}
      </Notification>
    ) : (
      <table className="card-table table is-hoverable is-fullwidth">
        <thead>
          <tr>
            <th>{t("scm-review-plugin.config.branchProtection.exceptions.exceptedUserOrGroup")}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {exceptions.map(exception => (
            <tr>
              <td>
                <PermissionIcon exception={exception} /> {exception.name}
              </td>
              <VCenteredTd className="is-darker">
                <a
                  className="level-item"
                  onClick={() => deleteException(exception)}
                  title={t("scm-review-plugin.config.branchProtection.exceptions.deleteException")}
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
        <label className="label">{t("scm-review-plugin.config.branchProtection.exceptions.exceptionType")}</label>
        <div className="field is-grouped">
          <div className="control">
            <Radio
              label={t("scm-review-plugin.config.branchProtection.exceptions.userException")}
              name="exception_scope"
              value="USER_PERMISSION"
              checked={!exceptionToAddIsGroup}
              onChange={changeToUserException}
            />
            <Radio
              label={t("scm-review-plugin.config.branchProtection.exceptions.groupException")}
              name="exception_scope"
              value="GROUP_PERMISSION"
              checked={exceptionToAddIsGroup}
              onChange={changeToGroupException}
            />
          </div>
        </div>
      </div>
      <div className="column">{renderAutocomplete()}</div>
      <div className="column is-narrow">
        <Button
          title={t("scm-review-plugin.config.branchProtection.exceptions.add.helpText")}
          label={t("scm-review-plugin.config.branchProtection.exceptions.add.label")}
          disabled={readOnly || !nameToAdd}
          icon="plus"
          action={addException}
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

export default ExceptionsList;
