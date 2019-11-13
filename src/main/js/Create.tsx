import React from "react";
import { ErrorNotification, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import CreateForm from "./CreateForm";
import styled from "styled-components";
import { BasicPullRequest } from "./types/PullRequest";
import { createPullRequest } from "./pullRequest";
import { Trans, WithTranslation, withTranslation } from "react-i18next";
import PullRequestInformation from "./PullRequestInformation";
import { RouteComponentProps, withRouter } from "react-router-dom";
import queryString from "query-string";

const ControlButtons = styled.div`
  padding-top: 1.5em;
`;

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    userAutocompleteLink: string;
  };

type State = {
  pullRequest?: BasicPullRequest;
  loading: boolean;
  error?: Error;
  disabled: boolean;
};

class Create extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      disabled: true
    };
  }

  pullRequestCreated = () => {
    const { history, repository } = this.props;
    history.push(`/repo/${repository.namespace}/${repository.name}/pull-requests`);
  };

  submit = () => {
    const { pullRequest } = this.state;
    const { repository } = this.props;

    this.setState({
      loading: true
    });

    createPullRequest(repository._links.pullRequest.href, pullRequest).then(result => {
      if (result.error) {
        this.setState({
          loading: false,
          error: result.error
        });
      } else {
        this.setState({
          loading: false
        });
        this.pullRequestCreated();
      }
    });
  };

  verify = (pullRequest: BasicPullRequest) => {
    const { source, target, title } = pullRequest;
    if (source && target && title) {
      return source !== target;
    }
    return false;
  };

  handleFormChange = (pullRequest: BasicPullRequest) => {
    this.setState({
      pullRequest,
      disabled: !this.verify(pullRequest)
    });
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
        />
      );
    }
    const subtitle = (
      <Trans
        i18nKey="scm-review-plugin.create.subtitle"
        values={{
          repositoryName: repository.name
        }}
      />
    );

    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.create.title")} />
          <Subtitle subtitle={subtitle} />

          {notification}

          <CreateForm
            repository={repository}
            userAutocompleteLink={this.props.userAutocompleteLink}
            onChange={this.handleFormChange}
            source={params.source}
            target={params.target}
          />

          {information}

          <ControlButtons>
            <SubmitButton
              label={t("scm-review-plugin.create.submitButton")}
              action={this.submit}
              loading={loading}
              disabled={disabled}
            />
          </ControlButtons>
        </div>
      </div>
    );
  }
}

export default withRouter(withTranslation("plugins")(Create));
