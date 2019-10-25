import React from "react";
import { Link, withRouter } from "react-router-dom";
import styled from "styled-components";
import { DateFromNow, Tag } from "@scm-manager/ui-components";
import { PullRequest } from "./../types/PullRequest";

const CellWithWordBreak = styled.td.attrs(props => ({
  className: "is-word-break"
}))`
  min-width: 10em;
`;

type Props = {
  pullRequest: PullRequest;
};

class PullRequestRow extends React.Component<Props> {
  renderLink(to: string, label: string) {
    return <Link to={to}>{label}</Link>;
  }

  render() {
    const { pullRequest } = this.props;
    const to = `pull-request/${pullRequest.id}/comments/`;
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
      </tr>
    );
  }
}

export default withRouter(PullRequestRow);
