import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { DiffButton } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { PullRequest } from "../types/PullRequest";
import { deleteReviewMark, postReviewMark } from "../pullRequest";

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
    postReviewMark((pullRequest._links.reviewMark as Link).href, this.determinePath());
    setCollapse(true);
    this.setState({ marked: true });
  };

  unmark = () => {
    const { pullRequest, setCollapse } = this.props;
    deleteReviewMark((pullRequest._links.reviewMark as Link).href, this.determinePath());
    setCollapse(false);
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
