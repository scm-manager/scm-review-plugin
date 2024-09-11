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
import { useTranslation, withTranslation, WithTranslation } from "react-i18next";
import { Modal } from "@scm-manager/ui-components";
import { Result } from "../types/EngineConfig";
import ModalRow from "./ModalRow";

type Props = WithTranslation & {
  result: Result[];
  failed: boolean;
  onClose: () => void;
};

const StatusModalView: FC<Props> = ({ result, failed, onClose }) => {
  const [t] = useTranslation("plugins");

  const body = (
    <>
      {result.map(r => (
        <ModalRow result={r} />
      ))}
    </>
  );
  const errors = result && result.length > 0 ? result.filter(r => r.failed).length : 0;
  const color = failed ? "warning" : "success";

  const modalTitle = failed
    ? t("scm-review-plugin.workflow.modal.failedTitle", {
        count: errors
      })
    : t("scm-review-plugin.workflow.modal.successTitle");

  return (
    <Modal
      title={
        <strong
          className={`has-text-${color === "warning" ? "warning-invert" : color === "success" ? "white" : "default"}`}
        >
          {modalTitle}
        </strong>
      }
      closeFunction={() => onClose()}
      body={body}
      active={true}
      headColor={color}
    />
  );
};

export default withTranslation("plugins")(StatusModalView);
