// @flow
import React from "react";
import {
  Title,
  Loading,
  ErrorPage,
  Subtitle,
  DateFromNow
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import { getPullRequest } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import PullRequestButton from "./PullRequestButton";
import injectSheet from "react-jss";
import classNames from "classnames";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  }
};

type Props = {
  repository: Repository,
  classes: any,
  t: string => string,
  match: any
};

type State = {
  pullRequest: PullRequest,
  error?: Error,
  loading: boolean
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      pullRequest: null
    };
  }

  componentDidMount(): void {
    const { repository } = this.props;
    const pullRequestNumber = this.props.match.params.pullRequestNumber;
    const url =
      "/pull-requests/" +
      repository.namespace +
      "/" +
      repository.name +
      "/" +
      pullRequestNumber; //TODO: use link if available
    getPullRequest(url).then(response => {
      if (response.error) {
        this.setState({
          error: response.error,
          loading: false
        });
      } else {
        this.setState({
          pullRequest: response,
          loading: false
        });
      }
    });
  }

  render() {
    const { repository, t, classes } = this.props;
    const { pullRequest, error, loading } = this.state;
    let description = null;
    if (error) {
      return (
        <ErrorPage
          title={t("scm-review-plugin.show-pull-request.error-title")}
          subtitle={t("scm-review-plugin.show-pull-request.error-subtitle")}
          error={error}
        />
      );
    }

    if (!pullRequest || loading) {
      return <Loading />;
    }

    if (pullRequest.description) {
      description = (
        <div className="media">
          <div className="media-left">
            {t("scm-review-plugin.show-pull-request.description")}
          </div>
          <div className="media-content">{pullRequest.description}</div>
        </div>
      );
    }

    return (
      <div className="columns">
        <div className="column">
          <Title
            title={t("scm-review-plugin.create.title") + " #" + pullRequest.id}
          />

            <div className="media">
              <div className="media-content">
                <Subtitle subtitle={pullRequest.title} />
                <div>
                  <span className="tag is-light is-medium">{pullRequest.source}</span>{" "}
                  <i className="fas fa-long-arrow-alt-right" />{" "}
                  <span className="tag is-light is-medium">{pullRequest.target}</span>
                </div>
              </div>
              <div className="media-right">{pullRequest.status}</div>
            </div>

          {description}

          <div className={classNames("media", classes.bottomSpace)}>
            <div className="media-content">
              {pullRequest.author}
            </div>
            <div className="media-right"><DateFromNow date={pullRequest.creationDate} /></div>
          </div>
          <PullRequestInformation repository={repository}/>
          <PullRequestButton/>
        </div>
      </div>
    );
  }
}

export default withRouter(injectSheet(styles)(translate("plugins")(SinglePullRequest)));
