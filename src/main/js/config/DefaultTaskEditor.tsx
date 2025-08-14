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
import { useTranslation } from "react-i18next";
import { Level, MarkdownView, Notification } from "@scm-manager/ui-components";
import { CardList } from "@scm-manager/ui-layout";
import { Menu } from "@scm-manager/ui-overlays";
import { Icon } from "@scm-manager/ui-buttons";
import styled from "styled-components";
import AddDefaultTaskDialog from "./AddDefaultTaskDialog";
import EditDefaultTaskDialog from "./EditDefaultTaskDialog";
import { Field, Label } from "@scm-manager/ui-forms";

type DefaultTaskEditorProps = {
  defaultTasks: string[];
  addTask: (newDefaultTask: string) => void;
  editTask: (task: string, index: number) => void;
  removeTask: (defaultTask: string) => void;
};

const StyledCardList = styled(CardList)`
  height: min-content;
`;

const CardWrapper = styled.div`
  .empty-state,
  ul:empty {
    display: none !important;
  }
  ul:empty + .empty-state {
    display: block !important;
  }
`;

const NoScrollMarkdownView = styled(MarkdownView)`
  overflow-y: hidden;
`;

const DefaultTaskEditor: FC<DefaultTaskEditorProps> = ({ defaultTasks, addTask, editTask, removeTask }) => {
  const [t] = useTranslation("plugins");

  return (
    <Field>
      <Label id="default-tasks-label">{t("scm-review-plugin.config.defaultTasks.label")}</Label>
      <p className="mb-2">{t("scm-review-plugin.config.defaultTasks.information")}</p>
      <CardWrapper className="control">
        <StyledCardList aria-labelledby="default-tasks-label" className="input p-2">
          {defaultTasks.map(task => (
            <>
              <CardList.Card
                className="is-full-width"
                action={
                  <Menu>
                    <EditDefaultTaskDialog
                      task={task}
                      action={(t: string) => editTask(t, defaultTasks.indexOf(task))}
                    />
                    <Menu.Button onSelect={() => removeTask(task)}>
                      <Icon>trash</Icon>
                      {t("scm-review-plugin.config.defaultTasks.deleteTask")}
                    </Menu.Button>
                  </Menu>
                }
              >
                <CardList.Card.Row>
                  <NoScrollMarkdownView content={task} />
                </CardList.Card.Row>
              </CardList.Card>
            </>
          ))}
        </StyledCardList>
        <Notification type="info" className="empty-state">
          {t("scm-review-plugin.config.defaultTasks.noTasks")}
        </Notification>
      </CardWrapper>
      <Level right={<AddDefaultTaskDialog action={addTask} />} />
    </Field>
  );
};

export default DefaultTaskEditor;
