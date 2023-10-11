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

import React, { FC, useEffect, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Dialog } from "@scm-manager/ui-overlays";
import { Button } from "@scm-manager/ui-buttons";
import { createA11yId, MarkdownView } from "@scm-manager/ui-components";
import { Field, Label, Textarea } from "@scm-manager/ui-forms";
import styled from "styled-components";

const MarkDownWrapper = styled.pre`
  height: 10rem;
`;

const AddDefaultTaskDialog: FC<{
  action: (task: string) => void;
}> = ({ action }) => {
  const [t] = useTranslation("plugins");
  const [showDialog, setShowDialog] = useState(false);
  const [defaultTask, setDefaultTask] = useState("");
  const id = createA11yId("textarea");

  useEffect(() => {
    if (!showDialog) {
      setDefaultTask("");
    }
  }, [showDialog]);

  return (
    <Dialog
      open={showDialog}
      onOpenChange={setShowDialog}
      trigger={<Button className="mt-2">{t("scm-review-plugin.config.defaultTasks.addTask")}</Button>}
      title={t("scm-review-plugin.config.defaultTasks.modal.createTitle")}
      footer={close => {
        return [
          <Button key="cancel" onClick={close}>
            {t("scm-review-plugin.config.defaultTasks.modal.cancel")}
          </Button>,
          <Button
            onClick={() => {
              action(defaultTask);
              setShowDialog(false);
            }}
            variant="primary"
            disabled={defaultTask === ""}
            key="addTask"
          >
            {t("scm-review-plugin.config.defaultTasks.modal.addTask")}
          </Button>
        ];
      }}
    >
      <>
        <Field>
          <Label htmlFor={id}>{t("scm-review-plugin.config.defaultTasks.modal.createInput")}</Label>
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
    </Dialog>
  );
};

export default AddDefaultTaskDialog;
