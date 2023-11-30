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
import { Dialog } from "@scm-manager/ui-overlays";
import { Button } from "@scm-manager/ui-buttons";

type Props = {
  reopen: () => Promise<unknown>;
  loading: boolean;
};

const ReopenButton: FC<Props> = ({ reopen, loading }) => {
  const [t] = useTranslation("plugins");
  const [open, setOpen] = useState<boolean>();

  return (
    <Dialog
      open={open}
      onOpenChange={setOpen}
      title={t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.title")}
      description={t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.message.hint")}
      footer={[
        <Button key="submit" variant="primary" onClick={() => reopen().finally(() => setOpen(false))}>
          {t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.submit")}
        </Button>,
        <Dialog.CloseButton key="cancel" autoFocus>
          {t("scm-review-plugin.showPullRequest.reopenButton.confirmAlert.cancel")}
        </Dialog.CloseButton>
      ]}
      trigger={
        <Button isLoading={loading} color="warning">
          {t("scm-review-plugin.showPullRequest.reopenButton.buttonTitle")}
        </Button>
      }
    />
  );
};

export default ReopenButton;
