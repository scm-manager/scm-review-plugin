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
import { Repository, Branch, Link } from "@scm-manager/ui-types";
import { AddButton, Loading } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { getPullRequests } from "./pullRequest";
import { PullRequest, PullRequestCollection } from "./types/PullRequest";
import PullRequestTable from "./table/PullRequestTable";
import styled from "styled-components";

type Props = WithTranslation & {
  repository: Repository;
  branch: Branch;
};

type State = {
  pullRequests: PullRequestCollection;
  error?: Error;
  loading: boolean;
};

const HR = styled.hr`
  height: 3px;
`;

class CreatePullRequestButton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequests: { _embedded: { pullRequests: [] }, _links: {} },
      loading: true
    };
  }

  componentDidMount(): void {
    if (this.props.repository._links.pullRequest) {
      const url = (this.props.repository._links.pullRequest as Link).href;
      this.updatePullRequests(url);
    }
  }

  updatePullRequests = (url: string) => {
    getPullRequests(url)
      .then(response => {
        this.setState({
          pullRequests: response,
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

  render() {
    const { repository, branch, t } = this.props;
    const { loading, error, pullRequests } = this.state;

    if (!this.props.repository._links.pullRequest) {
      return null;
    }

    if (loading) {
      return <Loading />;
    }

    if (error) {
      return;
    }

    const matchingPullRequests = pullRequests._embedded.pullRequests.filter(
      (pr: PullRequest) => pr.source === branch.name
    );

    let existing = null;
    if (matchingPullRequests.length > 0) {
      existing = (
        <div>
          <PullRequestTable repository={repository} pullRequests={matchingPullRequests} />
        </div>
      );
    }
    return (
      <div>
        <HR />
        <h4>{t("scm-review-plugin.branch.header")}</h4>
        {existing}
        <AddButton
          label={t("scm-review-plugin.branch.createPullRequest")}
          link={`/repo/${repository.namespace}/${repository.name}/pull-requests/add?source=${branch.name}`}
        />
      </div>
    );
  }
}

export default withTranslation("plugins")(CreatePullRequestButton);
