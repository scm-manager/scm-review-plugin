//@flow
import React from "react";
import {
  Title,
  Loading,
  ErrorPage,
  DateFromNow,
  Notification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import { getPullRequest, merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
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
  loading: boolean,
  mergePossible?: boolean,
  mergeButtonLoading: boolean,
  rejectButtonLoading: boolean,
  showNotification: boolean
};

class SinglePullRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      pullRequest: null,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false
    };
  }

  componentDidMount(): void {
    this.fetchPullRequest(false);
  }

  fetchPullRequest(mergedPerformed: boolean) {
    const { repository } = this.props;
    const pullRequestNumber = this.props.match.params.pullRequestNumber;
    const url = repository._links.pullRequest.href + "/" + pullRequestNumber;
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
        if (mergedPerformed) {
          this.setState({
            showNotification: true
          });
        } else {
          this.getMergeDryRun(response);
        }
      }
    });
  }

  getMergeDryRun(pullRequest: PullRequest) {
    const { repository } = this.props;
    merge(repository._links.mergeDryRun.href, pullRequest).then(response => {
      if (response.conflict) {
        this.setState({ mergeButtonLoading: false, mergePossible: false });
      } else if (response.error) {
        this.setState({ error: true, mergeButtonLoading: false });
      } else {
        this.setState({ mergePossible: true, mergeButtonLoading: false });
      }
    });
  }

  performMerge = () => {
    const { repository } = this.props;
    const { pullRequest } = this.state;
    this.setMergeButtonLoadingState();
    merge(repository._links.merge.href, pullRequest).then(response => {
      if (response.error) {
        this.setState({ error: response.error, mergeButtonLoading: false });
      } else if (response.conflict) {
        this.setState({
          mergePossible: true,
          mergeButtonLoading: false
        });
      } else {
        this.setState({ loading: true, mergeButtonLoading: false });
        this.fetchPullRequest(true);
      }
    });
  };

  performReject = () => {
    this.setState({rejectButtonLoading: true});
    const {pullRequest} = this.state;
    reject(pullRequest).then(
      () => {
        this.setState({rejectButtonLoading: false});
        this.fetchPullRequest(false);
      }
    ).catch(
      cause => this.setState({error: new Error(`could not reject request: ${cause.message}`), rejectButtonLoading: false})
    )
  };

  setMergeButtonLoadingState = () => {
    this.setState({
      mergeButtonLoading: true
    });
  };

  onClose = () => {
    this.setState({
      ...this.state,
      showNotification: false,
      mergeConflict: false
    });
  };

  render() {
    const { repository, match, t, classes } = this.props;
    const {
      pullRequest,
      error,
      loading,
      mergeButtonLoading,
      mergePossible,
      rejectButtonLoading,
      showNotification
    } = this.state;
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
          <div className="media-content">
            {pullRequest.description.split("\n").map(line => {
              return (
                <span>
                  {line}
                  <br />
                </span>
              );
            })}
          </div>
        </div>
      );
    }

    let mergeNotification = null;
    if (showNotification) {
      mergeNotification = (
        <Notification
          type={"info"}
          children={t("scm-review-plugin.show-pull-request.notification")}
          onClose={() => this.onClose()}
        />
      );
    }

    let mergeButton = null;
    let rejectButton = null;
    if (pullRequest._links.reject) {
      rejectButton = <RejectButton reject={() => this.performReject()} loading={rejectButtonLoading}/>;
      mergeButton = (
        <MergeButton
          merge={() => this.performMerge()}
          mergePossible={mergePossible}
          loading={mergeButtonLoading}
          repository={repository}
          pullRequest={pullRequest}
        />
      );
    }

    return (
      <div className="columns">
        <div className="column">
          <Title title={" #" + pullRequest.id + " " + pullRequest.title} />

          {mergeNotification}

          <div className="media">
            <div className="media-content">
              <div>
                <span className="tag is-light is-medium">
                  {pullRequest.source}
                </span>{" "}
                <i className="fas fa-long-arrow-alt-right" />{" "}
                <span className="tag is-light is-medium">
                  {pullRequest.target}
                </span>
              </div>
            </div>
            <div className="media-right">{pullRequest.status}</div>
          </div>

          {description}

          <div className={classNames("media", classes.bottomSpace)}>
            <div className="media-content">{pullRequest.author}</div>
            <div className="media-right">
              <DateFromNow date={pullRequest.creationDate} />
            </div>
          </div>

          {rejectButton}
          {mergeButton}

          <PullRequestInformation baseURL={match.url} repository={repository} source={pullRequest.source} target={pullRequest.target}/>
        </div>
      </div>
    );
  }
}

export default withRouter(
  injectSheet(styles)(translate("plugins")(SinglePullRequest))
);
