import React from "react";
import { Link } from "react-router-dom";
import styled from "styled-components";
import { Repository } from "@scm-manager/ui-types";
import { DateFromNow, Tag } from "@scm-manager/ui-components";
import { PullRequest } from "../types/PullRequest";
import ReviewerIcon from "./ReviewerIcon";

const CellWithWordBreak = styled.td.attrs(props => ({
  className: "is-word-break"
}))`
  min-width: 10em;
`;

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
};

class PullRequestRow extends React.Component<Props> {
  renderLink(to: string, label: string) {
    return <Link to={to}>{label}</Link>;
  }

  render() {
    const { repository, pullRequest } = this.props;
    const to = `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments/`;
    return (
      <tr>
        <CellWithWordBreak>{this.renderLink(to, pullRequest.title)}</CellWithWordBreak>
        <CellWithWordBreak>{pullRequest.source}</CellWithWordBreak>
        <CellWithWordBreak>{pullRequest.target}</CellWithWordBreak>
        <td className="is-hidden-mobile">{pullRequest.author ? pullRequest.author.displayName : ""}</td>
        <td className="is-hidden-mobile">
          {pullRequest.creationDate ? <DateFromNow date={pullRequest.creationDate} /> : ""}
        </td>
        <td className="is-hidden-mobile">
          <Tag
            className="is-medium"
            color={pullRequest.status === "MERGED" ? "success" : pullRequest.status === "REJECTED" ? "danger" : "light"}
            label={pullRequest.status}
          />
        </td>
        <td className="is-hidden-mobile has-text-centered">
          <ReviewerIcon reviewers={pullRequest.reviewer} />
        </td>
      </tr>
    );
  }
}

export default PullRequestRow;
