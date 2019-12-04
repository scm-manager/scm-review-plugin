import React, {FC} from "react";
import { Icon } from "@scm-manager/ui-components";
import {Reviewer} from "../types/PullRequest";

type Props = {
  reviewers?: Reviewer[];
};

const ReviewerIcon: FC<Props> = ({ reviewers }) => {
  if (!reviewers) {
    return null;
  }

  switch (reviewers.length) {
    case 0:
      return null;
    case 1:
      return <Icon title={reviewers[0].displayName} name="user" />;
    case 2:
      return <Icon title={reviewers.map(r => r.displayName).join(", ")} name="user-friends" />;
    default:
      return <Icon title={reviewers.map(r => r.displayName).join(", ")} name="users" />;
  }
};

export default ReviewerIcon;
