// @flow
import React from "react";
import {
  Loading,
  ErrorPage,
  CreateButton
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import { getPullRequests } from "./pullRequest";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";

type Props = {
  repository: Repository,
  t: string => string
};

type State = {
  pullRequests: PullRequest[],
  error?: Error,
  loading: boolean,
  status: string
};

class PullRequestList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequests: null,
      loading: true,
      status: "ALL"
    };
  }

  componentDidMount(): void {
    const url = this.props.repository._links.pullRequest.href;
    this.updatePullRequests(url);
  }

  updatePullRequests = (url: string) => {
    getPullRequests(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequests: response,
          loading: false
        });
      }
    });
  };

  handleStatusChange = (status: string) => {
    this.setState({
      status: status
    });
    const url = `${this.props.repository._links.pullRequest.href}?status=${status}`;
    this.updatePullRequests(url);
  };

  render() {
    const {t} = this.props;
    const { loading, error, pullRequests, status } = this.state;

    if (error) {
      return (
        <ErrorPage
          title={t("scm-review-plugin.pull-requests.error-title")}
          subtitle={t("scm-review-plugin.pull-requests.error-subtitle")}
          error={error}
        />
      );
    }

    if (!pullRequests || loading) {
      return <Loading />;
    }

    const to = `pull-requests/add`;
    return (
      <>
        <div className={"is-pulled-right"}>
          <StatusSelector handleTypeChange={this.handleStatusChange} status={status ? status : "ALL"}/>
        </div>
        <PullRequestTable pullRequests={pullRequests}/>
        <CreateButton label={t("scm-review-plugin.pull-requests.createButton")} link={to} />
      </>
    );
  }
}

export default withRouter(translate("plugins")(PullRequestList));
