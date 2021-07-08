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
import { DiffButton } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { deleteReviewMark, postReviewMark } from "../pullRequest";
import { DiffState } from "./Diff";

type Props = WithTranslation & {
  pullRequest: PullRequest;
  oldPath: string;
  newPath: string;
  setReviewed: (filepath: string, reviewed: boolean) => void;
  diffState: DiffState;
};

type State = {
  marked: boolean;
};

class MarkReviewedButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      marked: props.diffState.reviewedFiles.some(mark => mark === this.determinePath())
    };
  }

  determinePath = () => {
    const { oldPath, newPath } = this.props;
    if (newPath !== "/dev/null") {
      return newPath;
    } else {
      return oldPath;
    }
  };

  mark = () => {
    const { pullRequest, setReviewed } = this.props;
    const filepath = this.determinePath();
    postReviewMark((pullRequest._links.reviewMark as Link).href, filepath);
    setReviewed(filepath, true);
    this.setState({ marked: true });
  };

  unmark = () => {
    const { pullRequest, setReviewed } = this.props;
    const filepath = this.determinePath();
    deleteReviewMark((pullRequest._links.reviewMark as Link).href, filepath);
    setReviewed(filepath, false);
    this.setState({ marked: false });
  };

  render() {
    const { pullRequest, t } = this.props;
    if (!pullRequest?._links?.reviewMark) {
      return null;
    }
    if (this.state.marked) {
      return (
        <DiffButton
          onClick={this.unmark}
          tooltip={t("scm-review-plugin.diff.markNotReviewed")}
          icon="clipboard-check"
        />
      );
    } else {
      return <DiffButton onClick={this.mark} tooltip={t("scm-review-plugin.diff.markReviewed")} icon="clipboard" />;
    }
  }
}

export default withTranslation("plugins")(MarkReviewedButton);
