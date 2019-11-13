import React from "react";
import { ErrorNotification, Loading, SubmitButton, Subtitle, Title } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { updatePullRequest } from "./pullRequest";
import { Trans, withTranslation, WithTranslation } from "react-i18next";
import { RouteComponentProps, withRouter } from "react-router-dom";
import EditForm from "./EditForm";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    userAutocompleteLink: string;
  };

type State = {
  modifiedPullRequest: PullRequest;
  loading: boolean;
  error?: Error;
};

class Edit extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      modifiedPullRequest: props.pullRequest
    };
  }

  pullRequestUpdated = () => {
    const { history, repository, pullRequest } = this.props;
    history.push({
      pathname: `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/comments`,
      state: {
        from: this.props.match.url + "/updated"
      }
    });
  };

  submit = () => {
    const { modifiedPullRequest } = this.state;

    this.setState({
      loading: true
    });

    updatePullRequest(modifiedPullRequest._links.update.href, modifiedPullRequest).then(result => {
      if (result.error) {
        this.setState({
          loading: false,
          error: result.error
        });
      } else {
        this.setState({
          loading: false
        });
        this.pullRequestUpdated();
      }
    });
  };

  handleFormChange = (value, name: string) => {
    this.setState({
      modifiedPullRequest: {
        ...this.state.modifiedPullRequest,
        [name]: value
      }
    });
  };

  render() {
    const { repository, t, pullRequest, userAutocompleteLink } = this.props;
    const { loading, error } = this.state;

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    if (loading) {
      return <Loading />;
    }

    const subtitle = (
      <Trans
        i18nKey="scm-review-plugin.edit.subtitle"
        values={{
          repositoryName: repository.name
        }}
      />
    );
    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.edit.title")} />
          <Subtitle subtitle={subtitle} />

          {notification}

          <EditForm
            description={pullRequest.description ? pullRequest.description : ""}
            title={pullRequest.title}
            reviewer={pullRequest.reviewer}
            userAutocompleteLink={userAutocompleteLink}
            handleFormChange={this.handleFormChange}
          />
          <SubmitButton label={t("scm-review-plugin.edit.submitButton")} action={this.submit} loading={loading} />
        </div>
      </div>
    );
  }
}

export default withRouter(withTranslation("plugins")(Edit));
