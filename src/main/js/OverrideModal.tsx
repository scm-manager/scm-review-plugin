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
import { Button, Modal, SubmitButton, Textarea } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { MergeCheck } from "./types/PullRequest";

type Props = WithTranslation & {
  close: () => void;
  proceed: (overrideMessage: string) => void;
  mergeCheck: MergeCheck;
};

type State = {
  overrideMessage: string;
};

class OverrideModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      overrideMessage: ""
    };
  }

  onChangeOverrideMessage = (newMessage: string) => {
    this.setState({ overrideMessage: newMessage });
  };

  render() {
    const { mergeCheck, close, proceed, t } = this.props;
    const { overrideMessage } = this.state;

    const footer = (
      <>
        <Button
          label={t("scm-review-plugin.showPullRequest.overrideModal.cancel")}
          action={() => close()}
          color="grey"
        />
        <SubmitButton
          color={"danger"}
          icon={"exclamation-triangle"}
          label={t("scm-review-plugin.showPullRequest.overrideModal.continue")}
          action={() => proceed(overrideMessage)}
        />
      </>
    );

    const obstacles = (
      <ul>
        {mergeCheck.mergeObstacles.map(obstacle => (
          <li>{t(obstacle.key)}</li>
        ))}
      </ul>
    );

    const body = (
      <>
        <div className={"content"}>
          {t("scm-review-plugin.showPullRequest.overrideModal.introduction")}
          {obstacles}
          <p>{t("scm-review-plugin.showPullRequest.overrideModal.addMessageText")}</p>
        </div>
        <Textarea onChange={this.onChangeOverrideMessage} />
      </>
    );

    return (
      <Modal
        title={t("scm-review-plugin.showPullRequest.overrideModal.title")}
        active={true}
        body={body}
        closeFunction={close}
        footer={footer}
        headColor={"danger"}
      />
    );
  }
}

export default withTranslation("plugins")(OverrideModal);
