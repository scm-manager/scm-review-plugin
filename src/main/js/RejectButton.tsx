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
      <hr />
      <Textarea
        label={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.label")}
        helpText={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.help")}
        value={message}
        placeholder={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message.placeholder")}
        onChange={(event) => setMessage(event.target.value)}
        ref={initialFocusRef}
      />
    </>
  );

  const footer = (
    <>
      <SubmitButton
        label={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.submit")}
        action={() => reject(message)}
        loading={loading}
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
