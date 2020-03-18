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
import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, confirmAlert } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  reject: () => void;
  loading: boolean;
};

class RejectButton extends React.Component<Props> {
  confirmReject = () => {
    const { t, reject } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.title"),
      message: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.submit"),
          onClick: () => reject()
        },
        {
          label: t("scm-review-plugin.showPullRequest.rejectButton.confirmAlert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  render() {
    const { loading, t } = this.props;
    const color = "warning";
    const action = this.confirmReject;
    return (
      <p className="control">
        <Button
          label={t("scm-review-plugin.showPullRequest.rejectButton.buttonTitle")}
          action={action}
          loading={loading}
          color={color}
        />
      </p>
    );
  }
}

export default withTranslation("plugins")(RejectButton);
