import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";
import { PullRequest } from "../types/PullRequest";

type Props = WithTranslation & {
  pullRequest: PullRequest;
  path: string;
};

class MarkReviewedButton extends React.Component<Props> {
  mark = () => {
  };

  render() {
    const { pullRequest, path, t } = this.props;
    if (pullRequest.markedAsReviewed.some(mark => mark === path)) {
      return (
        <Button
          action={this.mark}
          label={t("scm-review-plugin.comment.unmarkReviewed")}
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
