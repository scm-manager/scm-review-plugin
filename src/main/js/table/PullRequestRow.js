// @flow
import React from "react";
import { Link, withRouter } from "react-router-dom";
import type { PullRequest } from "./../types/PullRequest";
import { DateFromNow } from "@scm-manager/ui-components";
import injectSheet from "react-jss";

const styles = {
  wordBreakColumn: {
    WebkitHyphens: "auto",
    MozHyphens: "auto",
    MsHyphens: "auto",
    hypens: "auto",
    wordBreak: "break-all",
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
        <td className={classes.wordBreakColumn}>
          {this.renderLink(to, pullRequest.title)}
        </td>
        <td className={classes.wordBreakColumn}>{pullRequest.source}</td>
        <td className={classes.wordBreakColumn}>{pullRequest.target}</td>
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
