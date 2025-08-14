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
import { Trans, useTranslation } from "react-i18next";
import { Menu } from "@scm-manager/ui-overlays";
import { Button, Icon } from "@scm-manager/ui-buttons";
import { MarkdownView, createA11yId } from "@scm-manager/ui-components";
import { Field, Label, Textarea } from "@scm-manager/ui-forms";
import styled from "styled-components";

const MarkDownWrapper = styled.pre`
  height: 10rem;
`;

const EditDefaultTaskDialog: FC<{
  task: string;
  action: (task: string) => void;
}> = ({ task, action }) => {
  const [t] = useTranslation("plugins");
  const [defaultTask, setDefaultTask] = useState(task);
  const id = createA11yId("textarea");

  return (
    <Menu.DialogButton
      dialogContent={
        <>
          <Field>
            <Label htmlFor={id}>{t("scm-review-plugin.config.defaultTasks.modal.editInput")}</Label>
            <p className="mb-2">
              <Trans t={t} i18nKey="scm-review-plugin.config.defaultTasks.modal.markdownInformation">
                <a href="https://daringfireball.net/projects/markdown/syntax.text" target="_blank" rel="noreferrer">
                  MarkDown Syntax
                </a>
              </Trans>
            </p>
            <Textarea value={defaultTask} onChange={event => setDefaultTask(event.target.value)} id={id} />
          </Field>
          <Label>{t("scm-review-plugin.config.defaultTasks.modal.preview")}</Label>
          <MarkDownWrapper>
            <MarkdownView content={defaultTask} />
          </MarkDownWrapper>
        </>
      }
      title={t("scm-review-plugin.config.defaultTasks.modal.editTitle")}
      footer={close => {
        return [
          <Button key="cancel" onClick={close}>
            {t("scm-review-plugin.config.defaultTasks.modal.cancel")}
          </Button>,
          <Button
            onClick={() => {
              action(defaultTask);
              close();
            }}
            variant="primary"
            disabled={defaultTask === ""}
            key="editTask"
          >
            {t("scm-review-plugin.config.defaultTasks.modal.editTask")}
          </Button>
        ];
      }}
    >
      <Icon>edit</Icon>
      {t("scm-review-plugin.config.defaultTasks.editTask")}
    </Menu.DialogButton>
  );
};

export default EditDefaultTaskDialog;
