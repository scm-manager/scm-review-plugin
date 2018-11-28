// @flow
import React from "react";
import {
  ErrorNotification, SubmitButton, Subtitle, Title
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import {getPullRequest} from "./pullRequest";

type Props = {
  repository: Repository,
  classes: any,
  t: string => string,
  match: any
};

type State = {
  pullRequestNumber: number,
  pullRequest: PullRequest,
  error?: Error,
  loading: boolean
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      pullRequestNumber : this.props.match.params.pullRequestNumber,
      loading: true,
      pullRequest: null
    };
  }

  componentDidMount(): void {
    const {repository} = this.props;
    const {pullRequestNumber} = this.state;
    const url = "/pull-requests/" + repository.namespace + "/" + repository.name + "/" + pullRequestNumber;
    getPullRequest(url)
      .then(response => {
        console.log(response);
        if(response.error){
          this.setState({
            error: response.error,
            loading: false
          })
        }
        else {
          this.setState({
            pullRequest: response,
            loading: false
          })
        }
      });
  }

  render() {
    const {repository, t} = this.props;
    const { pullRequestNumber } = this.state;
    console.log(this.state);
    return (
      <div className="columns">
        <div className="column">
          <Title title={t("scm-review-plugin.create.title") + pullRequestNumber + ".."} />
          <div className="tabs">
            <ul>
              <li className="is-active">
                <a>Commits</a>
              </li>
              <li>
                <a>Diff</a>
              </li>
            </ul>
          </div>

          <p>The Changelog ...</p>

        </div>
      </div>
    );
  }
}

export default withRouter(translate("plugins")(SinglePullRequest));
