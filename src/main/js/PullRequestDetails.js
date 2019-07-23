// @flow
import React from "react";
import injectSheet from "react-jss";
import { translate } from "react-i18next";
import { withRouter } from "react-router-dom";
import classNames from "classnames";
import type { History } from "history";
import type { Repository } from "@scm-manager/ui-types";
import {
  DateFromNow,
  Loading,
  Notification,
  Title,
  ErrorNotification,
  Tooltip,
  MarkdownView,
  Button
} from "@scm-manager/ui-components";
import type { PullRequest } from "./types/PullRequest";
import {
  getSubscription,
  handleSubscription,
  merge,
  reject
} from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
import {ExtensionPoint} from "@scm-manager/ui-extensions";

const styles = {
  userListMargin: {
    marginBottom: "1.5em"
  },
  userLabelAlignment: {
    textAlign: "left",
    marginRight: 0,
    minWidth: "5.5em"
  },
  userFieldFlex: {
    flexGrow: 8
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
  loadingSubscription: boolean,
  mergeHasNoConflict?: boolean,
  targetBranchDeleted?: boolean,
  mergeButtonLoading: boolean,
  rejectButtonLoading: boolean,
  showNotification: boolean,
  subscriptionLabel: string,
  subscriptionLink: string,
  subscriptionColor: string
};

class PullRequestDetails extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      loadingSubscription: true,
      subscriptionColor: "success",
      pullRequest: this.props.pullRequest,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    this.getMergeDryRun(pullRequest);
    if (
      pullRequest &&
      pullRequest._links.subscription &&
      pullRequest._links.subscription.href
    ) {
      this.getSubscription(pullRequest);
    }
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

  getSubscription(pullRequest: PullRequest) {
    if (
      pullRequest &&
      pullRequest._links.subscription &&
      pullRequest._links.subscription.href
    ) {
      getSubscription(pullRequest._links.subscription.href).then(response => {
        if (response.error) {
          this.setState({
            error: true,
            loadingSubscription: false
          });
        } else {
          if (response._links.subscribe) {
            this.setState({
              loadingSubscription: false,
              subscriptionLabel: "subscribe",
              subscriptionLink: response._links.subscribe.href,
              subscriptionColor: "success"
            });
          } else if (response._links.unsubscribe) {
            this.setState({
              loadingSubscription: false,
              subscriptionLabel: "unsubscribe",
              subscriptionLink: response._links.unsubscribe.href,
              subscriptionColor: "warning"
            });
          }
        }
      });
    }
  }

  handleSubscription = () => {
    const { pullRequest } = this.props;
    const { subscriptionLink } = this.state;
    this.setState({ loadingSubscription: true });
    handleSubscription(subscriptionLink).then(response => {
      this.setState({
        error: response.error
      });
      this.getSubscription(pullRequest);
    });
  };

  getMergeDryRun(pullRequest: PullRequest) {
    const { repository } = this.props;
    if (
      repository._links.mergeDryRun &&
      repository._links.mergeDryRun.href &&
      pullRequest.status === "OPEN"
    ) {
      merge(repository._links.mergeDryRun.href, pullRequest).then(response => {
        if (response.conflict) {
          this.setState({
            mergeButtonLoading: false,
            loading: false,
            mergeHasNoConflict: false
          });
        } else if (response.notFound) {
          this.setState({
            mergeButtonLoading: false,
            loading: false,
            targetBranchDeleted: true
          });
        } else if (response.error) {
          this.setState({
            error: true,
            loading: false,
            mergeButtonLoading: false
          });
        } else {
          this.setState({
            mergeHasNoConflict: true,
            targetBranchDeleted: false,
            loading: false,
            mergeButtonLoading: false
          });
        }
      });
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
          mergeHasNoConflict: true,
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
      mergeHasNoConflict,
      targetBranchDeleted,
      rejectButtonLoading,
      showNotification,
      subscriptionLabel,
      subscriptionLink,
      subscriptionColor,
      loadingSubscription
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
            <MarkdownView
              className="content"
              content={pullRequest.description}
            />
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
      if (!!repository._links.merge) {
        mergeButton = targetBranchDeleted ? null : (
          <MergeButton
            merge={() => this.performMerge()}
            mergeHasNoConflict={mergeHasNoConflict}
            loading={mergeButtonLoading}
            repository={repository}
            pullRequest={pullRequest}
          />
        );
      }
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
        <div className="media-right">
          <Button link={toEdit} color="primary" className="reduced-mobile">
            <span className="icon is-small">
              <i className="fas fa-edit" />
            </span>
            <span>{t("scm-review-plugin.edit.button")}</span>
          </Button>
        </div>
      );
    }

    const subscription = subscriptionLink ? (
      <div className="level-right">
        <div className="level-item">
          <Button
            label={t("scm-review-plugin.edit." + subscriptionLabel)}
            action={this.handleSubscription}
            loading={loadingSubscription}
            color={subscriptionColor}
          />
        </div>
      </div>
    ) : (
      ""
    );

    const targetBranchDeletedWarning = targetBranchDeleted ? (
      <span className="icon has-text-warning">
        <Tooltip
          className={classes.tooltip}
          message={t("scm-review-plugin.show-pull-request.targetDeleted")}
        >
          <i className="fas fa-exclamation-triangle" />
        </Tooltip>
      </span>
    ) : null;

    const author = (
      <div className="field is-horizontal">
        <div
          className={classNames(
            classes.userLabelAlignment,
            "field-label is-inline-flex"
          )}
        >
          {t("scm-review-plugin.pull-request.author")}:
        </div>
        <div
          className={classNames(
            classes.userFieldFlex,
            "field-body is-inline-flex"
          )}
        >
          <div className={"user"}>{pullRequest.author.displayName}</div>
          &nbsp;
          <DateFromNow date={pullRequest.creationDate} />
        </div>
      </div>
    );
    const reviewerList = (
      <>
        {pullRequest.reviewer.length > 0 ? (
          <div className="field is-horizontal">
            <div
              className={classNames(
                classes.userLabelAlignment,
                "field-label is-inline-flex"
              )}
            >
              {t("scm-review-plugin.pull-request.reviewer")}:
            </div>
            <div
              className={classNames(
                classes.userFieldFlex,
                "field-body is-inline-flex"
              )}
            >
              <ul className="is-separated">
                {pullRequest.reviewer.map(reviewer => {
                  return (
                    <li className={"user"} key={reviewer.id}>
                      {reviewer.displayName}
                    </li>
                  );
                })}
              </ul>
            </div>
          </div>
        ) : (
          ""
        )}
      </>
    );
    return (
      <div className="columns">
        <div className="column">
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
                {targetBranchDeletedWarning}
              </div>
            </div>
            <div className="media-right">
              <span
                className={classNames(
                  "tag",
                  "is-medium",
                  pullRequest.status === "MERGED"
                    ? "is-success"
                    : pullRequest.status === "REJECTED"
                    ? "is-danger"
                    : ""
                )}
              >
                {pullRequest.status}
              </span>
            </div>
          </div>
          
          {description}
          <ExtensionPoint
            name={"reviewPlugin.pullrequest.top"}
            renderAll={true}
            props={{repository, pullRequest}}
          />
          <div className={classNames(classes.userListMargin, "media")}>
            <div className="media-content">
              {author}
              {reviewerList}
            </div>
          </div>

          <div className="level">
            <div className="level-left">
              <div className="level-item">{rejectButton}</div>
              <div className="level-item">{mergeButton}</div>
            </div>
            {subscription}
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
