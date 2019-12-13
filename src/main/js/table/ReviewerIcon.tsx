import React, { FC } from "react";
import { Icon, Tooltip } from "@scm-manager/ui-components";
import { Reviewer } from "../types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";

type Props = WithTranslation & {
  reviewers?: Reviewer[];
};

const chooseIcon = (length: number): string => {
  switch (length) {
    case 1:
      return "user";
    case 2:
      return "user-friends";
    default:
      return "users";
  }
};

const createTooltipMessage = (reviewers: Reviewer[]) => {
  return reviewers.map(r => r.displayName).join(", ");
};

const ReviewerIcon: FC<Props> = ({ reviewers, t }) => {
  if (!reviewers || reviewers.length === 0) {
    return null;
  }

  const icon = chooseIcon(reviewers.length);
  const message = t("scm-review-plugin.pullRequest.reviewer") + ": " + createTooltipMessage(reviewers);
  return (
    <Tooltip location="top" message={message}>
      <Icon name={icon} />
    </Tooltip>
  );
};

export default withTranslation("plugins")(ReviewerIcon);
