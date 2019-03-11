// @flow
import React from "react";
import {
  DateFromNow,
  Loading,
  Notification,
  Title,
  ErrorNotification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import type { PullRequest } from "./types/PullRequest";
import { translate } from "react-i18next";
import { Link, withRouter } from "react-router-dom";
import { merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import type { History } from "history";
import injectSheet from "react-jss";
import classNames from "classnames";
import RejectButton from "./RejectButton";

const styles = {
  bottomSpace: {
    marginBottom: "1.5em"
  },
  tagShorter: {
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap",
    maxWidth: "25em"
  },
  borderTop: {
    borderTop: "none !important"
  }
};

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  classes: any,
  t: string => string,
  match: any,
  history: History
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

class PullRequestDetails extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      pullRequest: this.props.pullRequest,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    this.getMergeDryRun(pullRequest);
  }

  updatePullRequest = () => {
    const { history, match } = this.props;
    history.push({
      pathname: `${match.url}/comments`,
      state: {
        from: this.props.match.url + "/updated"
      }
    });
  };

  getMergeDryRun(pullRequest: PullRequest) {
    const { repository } = this.props;
    if (repository._links.mergeDryRun && repository._links.mergeDryRun.href) {
      merge(repository._links.mergeDryRun.href, pullRequest).then(response => {
        if (response.conflict || response.notFound) {
          this.setState({
            mergeButtonLoading: false,
            loading: false,
            mergePossible: false
          });
        } else if (response.error) {
          this.setState({
            error: true,
            loading: false,
            mergeButtonLoading: false
          });
        } else {
          this.setState({
            mergePossible: true,
            loading: false,
            mergeButtonLoading: false
          });
        }
      });
    } else {
      // TODO: what to do if the link does not exists?
    }
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
        this.setState({
          loading: true,
          showNotification: true,
          mergeButtonLoading: false
        });
        this.updatePullRequest();
      }
    });
  };

  performReject = () => {
    this.setState({ rejectButtonLoading: true });
    const { pullRequest } = this.state;
    reject(pullRequest)
      .then(() => {
        this.setState({ rejectButtonLoading: false });
        this.updatePullRequest();
      })
      .catch(cause =>
        this.setState({
          error: new Error(`could not reject request: ${cause.message}`),
          rejectButtonLoading: false
        })
      );
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

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!pullRequest || loading) {
      return <Loading />;
    }

    let description = null;
    if (pullRequest.description) {
      description = (
        <div className="media">
          <div className="media-content">
            {pullRequest.description.split("\n").map(line => {
              return (
                <span className="is-word-break">
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
      rejectButton = (
        <RejectButton
          reject={() => this.performReject()}
          loading={rejectButtonLoading}
        />
      );
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

    let editButton = null;
    if (pullRequest._links.update && pullRequest._links.update.href) {
      const toEdit =
        "/repo/" +
        repository.namespace +
        "/" +
        repository.name +
        "/pull-request/" +
        pullRequest.id +
        "/edit";
      editButton = (
        <a className="media-right">
          <span className="icon is-small">
            <Link to={toEdit}>
              <i className="fas fa-edit" />
            </Link>
          </span>
        </a>
      );
    }

    return (
      <div className="columns">
        <div className="column is-clipped">
          <div className="media">
            <div className="media-content">
              <Title title={" #" + pullRequest.id + " " + pullRequest.title} />
            </div>
            {editButton}
          </div>

          {mergeNotification}

          <div className={classNames(classes.borderTop, "media")}>
            <div className="media-content">
              <div>
                <span
                  className="tag is-light is-medium"
                  title={pullRequest.source}
                >
                  <span className={classes.tagShorter}>
                    {pullRequest.source}
                  </span>
                </span>{" "}
                <i className="fas fa-long-arrow-alt-right" />{" "}
                <span
                  className="tag is-light is-medium"
                  title={pullRequest.target}
                >
                  <span className={classes.tagShorter}>
                    {pullRequest.target}
                  </span>
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

          <div className="field is-grouped">
            {rejectButton}
            {mergeButton}
          </div>

          <PullRequestInformation
            pullRequest={pullRequest}
            baseURL={match.url}
            repository={repository}
            source={pullRequest.source}
            target={pullRequest.target}
            status={pullRequest.status}
          />
        </div>
      </div>
    );
  }
}

export default withRouter(
  injectSheet(styles)(translate("plugins")(PullRequestDetails))
);
