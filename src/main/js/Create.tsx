import React from "react";
import { ErrorNotification, Level, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Changeset, Link, Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import styled from "styled-components";
import { BasicPullRequest, PullRequest } from "./types/PullRequest";
import { createChangesetUrl, createPullRequest, getChangesets } from "./pullRequest";
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
  showBranchesValidationError: boolean;
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
      changesets: [],
      showBranchesValidationError: false
    };
  }

  fetchChangesets = (pullRequest: BasicPullRequest) => {
    return getChangesets(createChangesetUrl(this.props.repository, pullRequest.source, pullRequest.target))
      .then((result: Changeset) => {
        this.setState({ changesets: result._embedded.changesets });
      })
      .catch((error: Error) => this.setState({ error, loading: false }));
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
      .then(response => {
        const location = response?.headers?.get('Location');
        return location?.substring(location.lastIndexOf('/') + 1);
      })
      .then((pullRequestId) => {
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

  verify = (pullRequest: BasicPullRequest) => {
    const { source, target, title } = pullRequest;
    if (source && target && title && this.state.changesets) {
      return source !== target && this.state.changesets.length > 0;
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
          disabled: !this.verify(pullRequest),
          showBranchesValidationError: !(this.state.changesets.length > 0)
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
    const { pullRequest, loading, error, disabled } = this.state;

    const url = this.props.location.search;
    const params = queryString.parse(url);

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    let information = null;
    if (pullRequest) {
      information = (
        <PullRequestInformation
          repository={repository}
          source={pullRequest.source}
          target={pullRequest.target}
          status="OPEN"
          baseURL={match.url}
          mergeHasNoConflict={true}
        />
      );
    }
    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.create.title")} />
          <Subtitle subtitle={t("scm-review-plugin.create.subtitle", { repositoryName: repository.name })} />

          {notification}

          <CreateForm
            repository={repository}
            userAutocompleteLink={this.props.userAutocompleteLink}
            onChange={this.handleFormChange}
            source={params.source}
            target={params.target}
            showBranchesValidationError={this.state.showBranchesValidationError}
          />

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
