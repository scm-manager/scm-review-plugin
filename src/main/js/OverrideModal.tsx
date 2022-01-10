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
  const [initialFocusNode, setInitialFocusNode] = useState<HTMLTextAreaElement | null>(null);

  const footer = (
    <>
      <SubmitButton
        color="danger"
        icon="exclamation-triangle"
        label={t("scm-review-plugin.showPullRequest.overrideModal.continue")}
        action={() => proceed(overrideMessage)}
      />
      <Button label={t("scm-review-plugin.showPullRequest.overrideModal.cancel")} action={() => close()} />
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
        ref={setInitialFocusNode}
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
      initialFocusNode={initialFocusNode}
    />
  );
};

export default OverrideModal;
