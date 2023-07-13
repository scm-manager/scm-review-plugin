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
import React, { FC, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal, SubmitButton, Textarea } from "@scm-manager/ui-components";

type Props = {
  reject: (message: string) => void;
  loading: boolean;
};

const RejectButton: FC<Props> = ({ reject, loading }) => {
  const [t] = useTranslation("plugins");
  const [showModal, setShowModal] = useState(false);
  const initialFocusRef = useRef<HTMLTextAreaElement>(null);
  const [message, setMessage] = useState("");

  const body = (
    <>
      <p>{t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.hint")}</p>
      <hr/>
      <Textarea
        label={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.label")}
        helpText={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.help")}
        value={message}
        placeholder={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.placeholder")}
        onChange={event => setMessage(event.target.value)}
        ref={initialFocusRef}
      />
    </>
  );

  const footer = (
    <>
      <SubmitButton
        label={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.submit")}
        action={() => reject(message)}
      />
      <Button
        label={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.cancel")}
        action={() => setShowModal(false)}
        color="grey"
      />
    </>
  );

  return (
    <>
      {showModal ? (
        <Modal
          title={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.title")}
          active={true}
          body={body}
          closeFunction={() => setShowModal(false)}
          footer={footer}
          initialFocusRef={initialFocusRef}
        />
      ) : null}
      <Button
        label={t("scm-review-plugin.showPullRequest.rejectButton.buttonTitle")}
        action={() => setShowModal(true)}
        loading={loading}
        color="warning"
      />
    </>
  );
};

export default RejectButton;
