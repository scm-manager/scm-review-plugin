// @flow
import React from "react";
import { Link } from "react-router-dom";
import type { PullRequest } from "./../types/PullRequest";
import {DateFromNow} from "@scm-manager/ui-components";

type Props = {
  pullRequest: PullRequest
};

export default class PullRequestRow extends React.Component<Props> {
  renderLink(to: string, label: string) {
    return <Link to={to}>{label}</Link>;
  }

  render() {
    const { pullRequest } = this.props;
    const to = `pull-request/${pullRequest.id}`;
    return (
      <tr>
        <td>{this.renderLink(to, pullRequest.title)}</td>
        <td>{pullRequest.source}</td>
        <td>
          {pullRequest.target}
        </td>
        <td className="is-hidden-mobile">{pullRequest.author}</td>
        <td className="is-hidden-mobile"><DateFromNow date={pullRequest.creationDate} /></td>
        <td className="is-hidden-mobile">
          {pullRequest.status}
        </td>
      </tr>
    );
  }
}
