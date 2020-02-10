import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { markAsReviewedOrNot } from "../pullRequest";

type Props = WithTranslation & {
  pullRequest: PullRequest;
  oldPath: string;
  newPath: string;
  setCollapse: (p: boolean) => void;
};

type State = {
  marked: boolean;
};

class MarkReviewedButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      marked: props.pullRequest.markedAsReviewed.some(mark => mark === this.determinePath())
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
    const { pullRequest, setCollapse } = this.props;
    markAsReviewedOrNot((pullRequest._links.markAsReviewed as Link).href, this.determinePath());
    setCollapse(true);
    this.setState({ marked: true });
  };

  unmark = () => {
    const { pullRequest, setCollapse } = this.props;
    markAsReviewedOrNot((pullRequest._links.markAsNotReviewed as Link).href, this.determinePath());
    setCollapse(false);
    this.setState({ marked: false });
  };

  render() {
    const { pullRequest, t } = this.props;
    if (!pullRequest?._links?.markAsReviewed) {
      return null;
    }
    if (this.state.marked) {
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
