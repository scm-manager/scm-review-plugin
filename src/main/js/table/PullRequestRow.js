// @flow
import React from "react";
import { Link, withRouter } from "react-router-dom";
import type { PullRequest } from "./../types/PullRequest";
import { DateFromNow } from "@scm-manager/ui-components";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  wordBreakMinWidth: {
    minWidth: "10em"
  }
};

type Props = {
  classes: any,
  pullRequest: PullRequest
};

class PullRequestRow extends React.Component<Props> {
  renderLink(to: string, label: string) {
    return <Link to={to}>{label}</Link>;
  }

  render() {
    const { classes, pullRequest } = this.props;
    const to = `pull-request/${pullRequest.id}/changesets/`;
    return (
      <tr>
        <td className={classNames(classes.wordBreakMinWidth, "is-word-break")}>
          {this.renderLink(to, pullRequest.title)}
        </td>
        <td className={classNames(classes.wordBreakMinWidth, "is-word-break")}>
          {pullRequest.source}
        </td>
        <td className={classNames(classes.wordBreakMinWidth, "is-word-break")}>
          {pullRequest.target}
        </td>
        <td className="is-hidden-mobile">
          {pullRequest.author ? pullRequest.author : ""}
        </td>
        <td className="is-hidden-mobile">
          {pullRequest.creationDate ? (
            <DateFromNow date={pullRequest.creationDate} />
          ) : (
            ""
          )}
        </td>
        <td className="is-hidden-mobile">
          {pullRequest.status ? pullRequest.status : ""}
        </td>
      </tr>
    );
  }
}

export default withRouter(injectSheet(styles)(PullRequestRow));
