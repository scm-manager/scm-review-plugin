import React from "react";
import { Title, Subtitle, SubmitButton, ErrorNotification, Loading } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { updatePullRequest } from "./pullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { withRouter, RouteComponentProps } from "react-router-dom";
import EditForm from "./EditForm";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    userAutocompleteLink: string;
    onChangePullRequest: (changes: Partial<PullRequest>) => void;
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
        this.pullRequestUpdated(modifiedPullRequest);
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
    this.props.onChangePullRequest({[name]: value})
  };

  render() {
    const { repository, t, pullRequest, userAutocompleteLink,onChangePullRequest } = this.props;
    const { loading, error } = this.state;

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    if (loading) {
      return <Loading />;
    }

    return (
      <div className="columns">
        <div className="column is-clipped">
          <Title title={t("scm-review-plugin.edit.title")} />
          <Subtitle subtitle={t("scm-review-plugin.edit.subtitle", { repositoryName: repository.name })} />

          {notification}

          <EditForm
            description={pullRequest.description ? pullRequest.description : ""}
            title={pullRequest.title}
            reviewer={pullRequest.reviewer}
            userAutocompleteLink={userAutocompleteLink}
            handleFormChange={this.handleFormChange}
            onChangePullRequest={onChangePullRequest}
          />
          <SubmitButton label={t("scm-review-plugin.edit.submitButton")} action={this.submit} loading={loading} />
        </div>
      </div>
    );
  }
}

export default withRouter(withTranslation("plugins")(Edit));
