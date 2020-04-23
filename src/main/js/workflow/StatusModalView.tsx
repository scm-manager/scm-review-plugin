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
      {result.map(result => (
        <ModalRow result={result} />
      ))}
    </>
  );
  const errors = result && result.length > 0 ? result.filter(r => r.failed).length : 0;
  const color = failed ? "warning" : "secondary";

  const modalTitle = failed
    ? t("scm-review-plugin.workflow.modal.failedTitle", {
        count: errors
      })
    : t("scm-review-plugin.workflow.modal.successTitle");

  return (
    <Modal
      title={
        <strong
          className={`has-text-${color === "warning" ? "warning-invert" : color === "secondary" ? "default" : "white"}`}
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
