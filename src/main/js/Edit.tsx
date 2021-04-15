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
import { ErrorNotification, Loading, SubmitButton, Subtitle, Title, Level } from "@scm-manager/ui-components";
import { Repository, Link } from "@scm-manager/ui-types";
import { PullRequest } from "./types/PullRequest";
import { updatePullRequest } from "./pullRequest";
import { WithTranslation, withTranslation } from "react-i18next";
import { RouteComponentProps, withRouter } from "react-router-dom";
import EditForm from "./EditForm";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    userAutocompleteLink: string;
    fetchPullRequest: () => void;
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

    updatePullRequest((modifiedPullRequest._links.update as Link).href, {...modifiedPullRequest, _embedded : {}})
      .then(() => {
        this.setState({
          loading: false
        });
        this.props.fetchPullRequest();
        this.pullRequestUpdated();
      })
      .catch(err => {
        this.setState({
          loading: false,
          error: err
        });
      });
  };

  handleFormChange = (value: any, name: string) => {
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

    return (
      <div className="columns">
        <div className="column">
          <Title title={t("scm-review-plugin.edit.title")} />
          <Subtitle subtitle={t("scm-review-plugin.edit.subtitle", { repositoryName: repository.name })} />

          {notification}

          <EditForm
            description={pullRequest.description ? pullRequest.description : ""}
            title={pullRequest.title}
            reviewer={pullRequest.reviewer}
            userAutocompleteLink={userAutocompleteLink}
            handleFormChange={this.handleFormChange}
          />
          <Level
            right={
              <SubmitButton label={t("scm-review-plugin.edit.submitButton")} action={this.submit} loading={loading} />
            }
          />
        </div>
      </div>
    );
  }
}

export default withRouter(withTranslation("plugins")(Edit));
