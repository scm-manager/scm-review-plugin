import React from "react";
import { Link } from "react-router-dom";
import styled from "styled-components";
import { Icon } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";

type Props = {
  repository: Repository;
  repositoryLink: string;
};

const PointerEventsLink = styled(Link)`
  pointer-events: all;
`;

class RepositoryPullRequestCardLink extends React.Component<Props> {
  render() {
    const { repository, repositoryLink } = this.props;
    if (repository._links["pullRequest"]) {
      return (
        <PointerEventsLink className="level-item" to={repositoryLink + "/pull-requests"}>
          <Icon className="fa-lg" name="fas fa-code-branch fa-rotate-180 fa-fw" color="inherit" />
        </PointerEventsLink>
      );
    } else {
      return null;
    }
  }
}

export default RepositoryPullRequestCardLink;
