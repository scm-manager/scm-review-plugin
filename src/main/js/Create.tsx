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
import { ErrorNotification, Level, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Changeset, Link, Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import styled from "styled-components";
import { BasicPullRequest, CheckResult } from "./types/PullRequest";
import { checkPullRequest, createChangesetUrl, createPullRequest, getChangesets } from "./pullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import PullRequestInformation from "./PullRequestInformation";
import { RouteComponentProps, withRouter } from "react-router-dom";
import queryString from "query-string";

const TopPaddingLevel = styled(Level)`
  padding-top: 1.5em;
`;

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    userAutocompleteLink: string;
  };

type State = {
  pullRequest: BasicPullRequest;
  loading: boolean;
  error?: Error;
  disabled: boolean;
  changesets: Changeset[];
  checkResult?: CheckResult;
};

class Create extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequest: {
        title: "",
        target: "",
        source: ""
      },
      loading: false,
      disabled: true,
      changesets: []
    };
  }

  fetchChangesets = (pullRequest: BasicPullRequest) => {
    const { repository } = this.props;

    return checkPullRequest((repository._links.pullRequestCheck as Link)?.href, pullRequest)
      .then(r => r.json())
      .then(checkResult => {
        this.setState({ checkResult: { status: checkResult.status } }, () => {
          if (this.state.checkResult?.status === "PR_VALID") {
            getChangesets(createChangesetUrl(repository, pullRequest.source, pullRequest.target))
              .then((result: Changeset) => {
                this.setState({ changesets: result._embedded.changesets });
              })
              .catch((error: Error) => this.setState({ error, loading: false }));
          }
        });
      })
      .catch(error => {
        this.setState({ error });
      });
  };

  pullRequestCreated = (pullRequestId: string) => {
    const { history, repository } = this.props;
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequestId}/comments`);
  };

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    this.setState({
      loading: true
    });

    if (!pullRequest) {
      throw new Error("illegal state, no pull request defined");
    }

    createPullRequest((repository._links.pullRequest as Link).href, pullRequest)
      .then(pullRequestId => {
        this.setState(
          {
            loading: false
          },
          pullRequestId ? () => this.pullRequestCreated(pullRequestId) : undefined
        );
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  isValid = () => {
    return this.state.checkResult?.status === "PR_VALID";
  };

  verify = (pullRequest: BasicPullRequest) => {
    const { source, target, title } = pullRequest;
    if (source && target && title) {
      return this.isValid();
    }
    return false;
  };

  shouldFetchChangesets = (pullRequest: BasicPullRequest) => {
    return (
      this.state.pullRequest?.source !== pullRequest.source || this.state.pullRequest.target !== pullRequest.target
    );
  };

  handleFormChange = (pullRequest: BasicPullRequest) => {
    if (this.shouldFetchChangesets(pullRequest)) {
      this.fetchChangesets(pullRequest).then(() => {
        this.setState({
          pullRequest,
          disabled: !this.verify(pullRequest)
        });
      });
    } else {
      this.setState({
        pullRequest,
        disabled: !this.verify(pullRequest)
      });
    }
  };

  render() {
    const { repository, match, t } = this.props;
    const { pullRequest, loading, error, disabled, checkResult } = this.state;

    const url = this.props.location.search;
    const params = queryString.parse(url);

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    let information = null;
    if (!loading && pullRequest?.source && pullRequest?.target) {
      information = (
        <PullRequestInformation
          repository={repository}
          source={pullRequest.source}
          target={pullRequest.target}
          status="OPEN"
          baseURL={match.url}
          mergeHasNoConflict={true}
          shouldFetchChangesets={this.isValid()}
        />
      );
    }
    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.create.title")} />
          <Subtitle subtitle={t("scm-review-plugin.create.subtitle", { repositoryName: repository.name })} />
          {notification}
          {!loading && (
            <CreateForm
              repository={repository}
              userAutocompleteLink={this.props.userAutocompleteLink}
              onChange={this.handleFormChange}
              source={params.source}
              target={params.target}
              checkResult={checkResult}
            />
          )}
          {information}
          <TopPaddingLevel
            right={
              <SubmitButton
                label={t("scm-review-plugin.create.submitButton")}
                action={this.submit}
                loading={loading}
                disabled={disabled}
              />
            }
          />
        </div>
      </div>
    );
  }
}

export default withRouter(withTranslation("plugins")(Create));
