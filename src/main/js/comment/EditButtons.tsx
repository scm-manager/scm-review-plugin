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
