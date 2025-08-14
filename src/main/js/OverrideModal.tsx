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
import { Button, Modal, SubmitButton, Textarea } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { MergeCheck } from "./types/PullRequest";
import styled from "styled-components";
import OverrideModalRow from "./OverrideModalRow";

type Props = {
  close: () => void;
  proceed: (overrideMessage: string) => void;
  mergeCheck?: MergeCheck;
};

const StyledModal = styled(Modal)`
  .modal-card-title {
    color: #fff;
  }
`;

const Description = styled.p`
  margin-top: 0.5rem;
  margin-bottom: -1rem;
`;

const OverrideModal: FC<Props> = ({ mergeCheck, close, proceed }) => {
  const [t] = useTranslation("plugins");
  const [overrideMessage, setOverrideMessage] = useState("");
  const initialFocusRef = useRef<HTMLTextAreaElement>(null);

  const footer = (
    <>
      <SubmitButton
        color="danger"
        icon="exclamation-triangle"
        label={t("scm-review-plugin.showPullRequest.overrideModal.continue")}
        action={() => proceed(overrideMessage)}
      />
      <Button label={t("scm-review-plugin.showPullRequest.overrideModal.cancel")} action={close} />
    </>
  );

  const obstacles = (
    <>
      {mergeCheck?.mergeObstacles.map(obstacle => (
        <OverrideModalRow result={{ rule: obstacle.key, failed: true }} useObstacleText={true} />
      ))}
    </>
  );

  const body = (
    <>
      <div className="content">
        {t("scm-review-plugin.showPullRequest.overrideModal.introduction")}
        {obstacles}
        <Description>{t("scm-review-plugin.showPullRequest.overrideModal.addMessageText")}</Description>
      </div>
      <Textarea
        onChange={event => setOverrideMessage(event.target.value)}
        onSubmit={() => proceed(overrideMessage)}
        ref={initialFocusRef}
      />
    </>
  );

  return (
    <StyledModal
      title={t("scm-review-plugin.showPullRequest.overrideModal.title")}
      active={true}
      body={body}
      closeFunction={close}
      footer={footer}
      headColor="danger"
      initialFocusRef={initialFocusRef}
    />
  );
};

export default OverrideModal;
