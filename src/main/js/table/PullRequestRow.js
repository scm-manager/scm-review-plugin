// @flow
import React from "react";
import { Link, withRouter } from "react-router-dom";
import injectSheet from "react-jss";
import classNames from "classnames";
import { DateFromNow, Tag } from "@scm-manager/ui-components";
import type { PullRequest } from "./../types/PullRequest";

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
    const to = `pull-request/${pullRequest.id}/comments/`;
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
          {pullRequest.author ? pullRequest.author.displayName : ""}
        </td>
        <td className="is-hidden-mobile">
          {pullRequest.creationDate ? (
            <DateFromNow date={pullRequest.creationDate} />
          ) : (
            ""
          )}
        </td>
        <td className="is-hidden-mobile">
          <Tag
            className="is-medium"
            color={
              pullRequest.status === "MERGED"
                ? "success"
                : pullRequest.status === "REJECTED"
                ? "danger"
                : "light"
            }
            label={pullRequest.status}
          />
        </td>
      </tr>
    );
  }
}

export default withRouter(injectSheet(styles)(PullRequestRow));
