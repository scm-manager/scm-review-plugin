/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import React from "react";
import { CreateButton, ErrorPage, Loading } from "@scm-manager/ui-components";
import { Repository, Link } from "@scm-manager/ui-types";
import { PullRequestCollection } from "./types/PullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { withRouter, RouteComponentProps } from "react-router-dom";
import { getPullRequests } from "./pullRequest";
import PullRequestTable from "./table/PullRequestTable";
import StatusSelector from "./table/StatusSelector";
import styled from "styled-components";

const ScrollingTable = styled.div`
  overflow-x: auto;
`;

type Props = RouteComponentProps &
  WithTranslation & {
    repository: Repository;
  };

type State = {
  pullRequests?: PullRequestCollection;
  error?: Error;
  loading: boolean;
  status: string;
};

class PullRequestList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequests: undefined,
      loading: true,
      status: "OPEN"
    };
  }

  componentDidMount(): void {
    const url = (this.props.repository._links.pullRequest as Link).href;
    this.updatePullRequests(url);
  }

  updatePullRequests = (url: string) => {
    getPullRequests(url)
      .then(pullRequests => {
        this.setState({
          pullRequests,
          loading: false
        });
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  handleStatusChange = (status: string) => {
    this.setState({
      status: status
    });
    const url = `${(this.props.repository._links.pullRequest as Link).href}?status=${status}`;
    this.updatePullRequests(url);
  };

  renderPullRequestTable = () => {
    const { repository } = this.props;
    const { pullRequests, status } = this.state;

    return (
      <div className="panel">
        <div className="panel-heading">
          <StatusSelector handleTypeChange={this.handleStatusChange} status={status ? status : "OPEN"} />
        </div>

        <ScrollingTable className="panel-block">
          <PullRequestTable
            repository={repository}
            pullRequests={pullRequests && pullRequests._embedded.pullRequests}
          />
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
          title={t("scm-review-plugin.pullRequests.errorTitle")}
          subtitle={t("scm-review-plugin.pullRequests.errorSubtitle")}
          error={error}
        />
      );
    }

    if (!pullRequests || loading) {
      return <Loading />;
    }

    const to = "pull-requests/add/changesets/";

    const createButton = pullRequests._links.create ? (
      <CreateButton label={t("scm-review-plugin.pullRequests.createButton")} link={to} />
    ) : null;

    return (
      <>
        {this.renderPullRequestTable()}
        {createButton}
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestList));
