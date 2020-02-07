import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";
import { PullRequest } from "../types/PullRequest";
import { markAsReviewedOrNot } from "../pullRequest";

type Props = WithTranslation & {
  pullRequest: PullRequest;
  path: string;
  setCollapse: (p: boolean) => void;
};

class MarkReviewedButton extends React.Component<Props> {
  mark = () => {
    const { pullRequest, path, setCollapse } = this.props;
    markAsReviewedOrNot(pullRequest._links.markAsReviewed.href, path);
    setCollapse(true);
  };

  unmark = () => {
    const { pullRequest, path, setCollapse } = this.props;
    markAsReviewedOrNot(pullRequest._links.markAsNotReviewed.href, path);
    setCollapse(false);
  };

  render() {
    const { pullRequest, path, t } = this.props;
    if (!pullRequest?._links?.markAsReviewed) {
      return null;
    }
    if (pullRequest.markedAsReviewed.some(mark => mark === path)) {
      return (
        <Button
          action={this.unmark}
          label={t("scm-review-plugin.comment.markNotReviewed")}
          icon="square"
          reducedMobile={true}
        />
      );
    } else {
      return (
        <Button
          action={this.mark}
          label={t("scm-review-plugin.comment.markReviewed")}
          icon="check-square"
          reducedMobile={true}
        />
      );
    }
  }
}

export default withTranslation("plugins")(MarkReviewedButton);
