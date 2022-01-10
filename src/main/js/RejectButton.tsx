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
import { useTranslation } from "react-i18next";
import { Button, ConfirmAlert } from "@scm-manager/ui-components";

type Props = {
  reject: () => void;
  loading: boolean;
};

const RejectButton: FC<Props> = ({ reject, loading }) => {
  const [t] = useTranslation("plugins");

  const [showModal, setShowModal] = useState(false);

  return (
    <p className="control">
      {showModal ? (
        <ConfirmAlert
          title={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.title")}
          message={t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message")}
          close={() => setShowModal(false)}
          buttons={[
            {
              className: "is-outlined",
              label: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.submit"),
              onClick: () => reject(),
              autofocus: true
            },
            {
              label: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.cancel"),
              onClick: () => setShowModal(false)
            }
          ]}
        />
      ) : null}
      <Button
        label={t("scm-review-plugin.showPullRequest.rejectButton.buttonTitle")}
        action={() => setShowModal(true)}
        loading={loading}
        color="warning"
      />
    </p>
  );
};

export default RejectButton;
