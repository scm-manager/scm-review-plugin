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
import React, { FC } from "react";
import { Level, SubmitButton, Button, confirmAlert } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { Comment } from "../types/PullRequest";

type Props = {
  comment: Comment;
  onSubmit: () => void;
  onCancel: () => void;
  changed: () => boolean;
};

const EditButtons: FC<Props> = ({ comment, onSubmit, onCancel, changed }) => {
  const { t } = useTranslation("plugins");

  const confirmCancelUpdate = () => {
    if (changed()) {
      confirmAlert({
        title: t("scm-review-plugin.comment.confirmCancelUpdateAlert.title"),
        message: t("scm-review-plugin.comment.confirmCancelUpdateAlert.message"),
        buttons: [
          {
            className: "is-outlined",
            label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.submit"),
            onClick: onCancel
          },
          {
            label: t("scm-review-plugin.comment.confirmCancelUpdateAlert.cancel"),
            onClick: () => null
          }
        ]
      });
    } else {
      onCancel();
    }
  };

  return (
    <Level
      right={
        <>
          <div className="level-item">
            <SubmitButton
              label={t("scm-review-plugin.comment.save")}
              action={onSubmit}
              disabled={(comment.comment || "").trim() === ""}
              scrollToTop={false}
            />
          </div>
          <div className="level-item">
            <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={confirmCancelUpdate} />
          </div>
        </>
      }
    />
  );
};

export default EditButtons;
