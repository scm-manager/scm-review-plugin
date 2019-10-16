// @flow
import React from "react";
import { Loading, ErrorPage, CreateButton } from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequestCollection } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import { getPullRequests } from "./pullRequest";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";
import styled from "styled-components";

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

type Props = {
  repository: Repository,
  t: string => string,
};

type State = {
  pullRequests: PullRequestCollection,
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
      status: "OPEN"
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
    const url = `${
      this.props.repository._links.pullRequest.href
    }?status=${status}`;
    this.updatePullRequests(url);
  };

  renderPullRequestTable = () => {
    const { pullRequests, status } = this.state;

    const availablePullRequests = pullRequests._embedded.pullRequests;

    return (
      <div className="panel">
        <div className="panel-heading">
          <StatusSelector
            handleTypeChange={this.handleStatusChange}
            status={status ? status : "OPEN"}
          />
        </div>

        <ScrollingTable className="panel-block">
          <PullRequestTable pullRequests={availablePullRequests} />
        </ScrollingTable>
      </div>
    );
  };

  render() {
    const { t } = this.props;
    const { loading, error, pullRequests } = this.state;

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

    const to = "pull-requests/add/changesets/";

    const createButton = pullRequests._links.create ? (
      <CreateButton
        label={t("scm-review-plugin.pull-requests.createButton")}
        link={to}
      />
    ) : null;

    return (
      <>
        {this.renderPullRequestTable()}
        {createButton}
      </>
    );
  }
}

export default withRouter(translate("plugins")(PullRequestList));
