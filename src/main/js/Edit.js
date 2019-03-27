// @flow
import React from "react";
import {
  Title,
  Subtitle,
  SubmitButton,
  ErrorNotification,
  Loading
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { updatePullRequest } from "./pullRequest";
import { translate, Trans } from "react-i18next";
import type { History } from "history";
import { withRouter } from "react-router-dom";
import EditForm from "./EditForm";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  userAutocompleteLink: string,
  t: string => string,
  match: any,
  history: History
};

type State = {
  modifiedPullRequest: PullRequest,
  loading: boolean,
  error?: Error
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
      pathname: `/repo/${repository.namespace}/${
        repository.name
      }/pull-request/${pullRequest.id}/comments`,
      state: {
        from: this.props.match.url + "/updated"
      }
    });
  };

  submit = () => {
    const { modifiedPullRequest } = this.state;

    this.setState({ loading: true });

    updatePullRequest(
      modifiedPullRequest._links.update.href,
      modifiedPullRequest
    ).then(result => {
      if (result.error) {
        this.setState({ loading: false, error: result.error });
      } else {
        this.setState({ loading: false });
        this.pullRequestUpdated();
      }
    });
  };

  handleFormChange = (values, name: string) => {
    this.setState({
      modifiedPullRequest: {
        ...this.state.modifiedPullRequest,
        [name]: value
      }
    });
  };

  render() {
    const { repository, t, pullRequest , userAutocompleteLink} = this.props;
    const { loading, error } = this.state;

    let notification = null;
    if (error) {
      notification = <ErrorNotification error={error} />;
    }

    if (loading) {
      return <Loading />;
    }

    let subtitle = (
      <Trans
        i18nKey="scm-review-plugin.edit.subtitle"
        values={{ repositoryName: repository.name }}
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
          <div>
            <SubmitButton
              label={t("scm-review-plugin.edit.submitButton")}
              action={this.submit}
              loading={loading}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default withRouter(translate("plugins")(Edit));
