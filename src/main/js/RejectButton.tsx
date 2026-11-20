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
import { Modal, Textarea } from "@scm-manager/ui-components";
import { Button } from "@scm-manager/ui-core";

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
      <Button
        type="submit"
        variant="primary"
        onClick={() => {
          reject(message);
          window.scrollTo(0, 0);
        }}
        isLoading={loading}
      >
        {t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.submit")}
      </Button>
      <Button onClick={() => setShowModal(false)}>
        {t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.cancel")}
      </Button>
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
      <Button variant="signal" onClick={() => setShowModal(true)} isLoading={loading}>
        {t("scm-review-plugin.showPullRequest.rejectButton.buttonTitle")}
      </Button>
    </>
  );
};

export default RejectButton;
