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
import { Trans, useTranslation } from "react-i18next";
import { ButtonGroup, Level, MarkdownView, Modal, Notification, Textarea } from "@scm-manager/ui-components";
import { CardList } from "@scm-manager/ui-layout";
import { Menu } from "@scm-manager/ui-overlays";
import { Button, Icon } from "@scm-manager/ui-buttons";
import styled from "styled-components";

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
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [task, setTask] = useState("");

  return (
    <>
      <div className="field">
        <label className="label" id="default-tasks-label">
          {t("scm-review-plugin.config.defaultTasks.label")}
        </label>
        <p className="mb-2">{t("scm-review-plugin.config.defaultTasks.information")}</p>
        <CardWrapper className="control">
          <StyledCardList aria-labelledby="default-tasks-label" className="input p-2">
            {defaultTasks.map(task => (
              <DefaultTaskEntry
                key={task}
                task={task}
                removeTask={removeTask}
                editTask={(task: string) => {
                  setTask(task);
                  setShowEditModal(true);
                }}
              />
            ))}
          </StyledCardList>
          <Notification type="info" className="empty-state">
            {t("scm-review-plugin.config.defaultTasks.noTasks")}
          </Notification>
        </CardWrapper>
        <Level
          right={
            <Button
              className="mt-2"
              onClick={() => {
                setTask("");
                setShowCreateModal(true);
              }}
            >
              {t("scm-review-plugin.config.defaultTasks.addTask")}
            </Button>
          }
        />
      </div>

      {showCreateModal ? (
        <DefaultTaskModal
          task={task}
          action={addTask}
          onClose={() => {
            setShowCreateModal(false);
            setTask("");
          }}
          mode="CREATE"
        />
      ) : null}
      {showEditModal ? (
        <DefaultTaskModal
          task={task}
          action={(t: string) => editTask(t, defaultTasks.indexOf(task))}
          onClose={() => {
            setShowEditModal(false);
            setTask("");
          }}
          mode="EDIT"
        />
      ) : null}
    </>
  );
};

const MarkDownWrapper = styled.pre`
  height: 10rem;
`;

const DefaultTaskModal: FC<{
  task: string;
  action: (task: string) => void;
  onClose: () => void;
  mode: "CREATE" | "EDIT";
}> = ({ task, action, onClose, mode }) => {
  const [t] = useTranslation("plugins");
  const [defaultTask, setDefaultTask] = useState(task);

  return (
    <Modal
      title={
        mode === "CREATE"
          ? t("scm-review-plugin.config.defaultTasks.modal.createTitle")
          : t("scm-review-plugin.config.defaultTasks.modal.editTitle")
      }
      active={true}
      body={
        <>
          <label className="label">
            {mode === "CREATE"
              ? t("scm-review-plugin.config.defaultTasks.modal.createInput")
              : t("scm-review-plugin.config.defaultTasks.modal.editInput")}
          </label>
          <p className="mb-2">
            <Trans
              t={t}
              i18nKey="scm-review-plugin.config.defaultTasks.modal.markdownInformation"
              components={[
                <a href="https://daringfireball.net/projects/markdown/syntax.text" target="_blank" rel="noreferrer">
                  MarkDown Syntax
                </a>
              ]}
            />
          </p>
          <Textarea value={defaultTask} onChange={setDefaultTask} />
          <label className="label">{t("scm-review-plugin.config.defaultTasks.modal.preview")}</label>
          <MarkDownWrapper>
            <MarkdownView content={defaultTask} />
          </MarkDownWrapper>
        </>
      }
      footer={
        <ButtonGroup>
          <Button onClick={onClose}>{t("scm-review-plugin.config.defaultTasks.modal.cancel")}</Button>
          <Button
            variant="primary"
            onClick={() => {
              action(defaultTask);
              setDefaultTask("");
              onClose();
            }}
          >
            {mode === "CREATE"
              ? t("scm-review-plugin.config.defaultTasks.modal.addTask")
              : t("scm-review-plugin.config.defaultTasks.modal.editTask")}
          </Button>
        </ButtonGroup>
      }
      closeFunction={onClose}
    />
  );
};

const DefaultTaskEntry: FC<{
  task: string;
  removeTask: (defaultTask: string) => void;
  editTask: (task: string) => void;
}> = ({ task, removeTask, editTask }) => {
  const [t] = useTranslation("plugins");
  return (
    <>
      <CardList.Card
        className="is-full-width"
        action={
          <Menu>
            <Menu.Button onSelect={() => editTask(task)}>
              <Icon>edit</Icon>
              {t("scm-review-plugin.config.defaultTasks.editTask")}
            </Menu.Button>
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
  );
};

export default DefaultTaskEditor;
