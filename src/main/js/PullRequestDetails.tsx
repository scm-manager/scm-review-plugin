import React from "react";
import styled from "styled-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { withRouter, RouteComponentProps } from "react-router-dom";
import { Repository } from "@scm-manager/ui-types";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import {
  DateFromNow,
  Loading,
  Notification,
  Title,
  ErrorNotification,
  Tooltip,
  MarkdownView,
  Button,
  Tag
} from "@scm-manager/ui-components";
import { PullRequest } from "./types/PullRequest";
import { getSubscription, handleSubscription, merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";

type Props = WithTranslation & RouteComponentProps & {
  repository: Repository;
  pullRequest: PullRequest;
};

type State = {
  pullRequest: PullRequest;
  error?: Error;
  loading: boolean;
  loadingSubscription: boolean;
  mergeHasNoConflict?: boolean;
  targetBranchDeleted?: boolean;
  mergeButtonLoading: boolean;
  rejectButtonLoading: boolean;
  showNotification: boolean;
  subscriptionIcon: string;
  subscriptionLabel: string;
  subscriptionLink: string;
};

const MediaContent = styled.div.attrs(props => ({
  className: "media-content"
}))`
  width: 100%;
  word-wrap: break-word;
`;

const UserLabel = styled.div.attrs(props => ({
  className: "field-label is-inline-flex"
}))`
  text-align: left;
  margin-right: 0;
  min-width: 5.5em;
`;

const UserField = styled.div.attrs(props => ({
  className: "field-body is-inline-flex"
}))`
  flex-grow: 8;
`;

const UserInline = styled.div`
  display: inline-block;
  font-weight: bold;
`;

const UserInlineListItem = styled.div`
  display: inline-block;
  font-weight: bold;
`;

const Container = styled.div`
  margin-bottom: 2rem;
  padding: 1rem;
  border: 1px solid #dbdbdb; // border
  border-radius: 4px;
`;

const MediaWithTopBorder = styled.div.attrs(props => ({
  className: "media"
}))`
  padding: 0 !important;
  border-top: none !important;
`;

const ShortTag = styled(Tag).attrs(props => ({
  className: "is-medium",
  color: "light"
}))`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 25em;
`;

const UserList = styled.div`
  margin-bottom: 1.5em;
`;

const LevelWrapper = styled.div`
  flex-flow: row wrap;

  & > .level-right {
    margin-left: auto;
  }
`;

class PullRequestDetails extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      loadingSubscription: true,
      pullRequest: this.props.pullRequest,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    this.getMergeDryRun(pullRequest);
    if (pullRequest && pullRequest._links.subscription && pullRequest._links.subscription.href) {
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
    if (pullRequest && pullRequest._links.subscription && pullRequest._links.subscription.href) {
      getSubscription(pullRequest._links.subscription.href).then(response => {
        if (response.error) {
          this.setState({
            error: response.error,
            loadingSubscription: false
          });
        } else {
          if (response._links.subscribe) {
            this.setState({
              loadingSubscription: false,
              subscriptionIcon: "plus",
              subscriptionLabel: "subscribe",
              subscriptionLink: response._links.subscribe.href
            });
          } else if (response._links.unsubscribe) {
            this.setState({
              loadingSubscription: false,
              subscriptionIcon: "minus",
              subscriptionLabel: "unsubscribe",
              subscriptionLink: response._links.unsubscribe.href
            });
          }
        }
      });
    }
  }

  handleSubscription = () => {
    const { pullRequest } = this.props;
    const { subscriptionLink } = this.state;
    this.setState({
      loadingSubscription: true
    });
    handleSubscription(subscriptionLink).then(response => {
      this.setState({
        error: response.error
      });
      this.getSubscription(pullRequest);
    });
  };

  getMergeDryRun(pullRequest: PullRequest) {
    const { repository } = this.props;
    if (repository._links.mergeDryRun && repository._links.mergeDryRun.href && pullRequest.status === "OPEN") {
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
            error: response.error,
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
        this.setState({
          error: response.error,
          mergeButtonLoading: false
        });
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
    this.setState({
      rejectButtonLoading: true
    });
    const { pullRequest } = this.state;
    reject(pullRequest)
      .then(() => {
        this.setState({
          rejectButtonLoading: false
        });
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
    const { repository, match, t } = this.props;
    const {
      pullRequest,
      error,
      loading,
      mergeButtonLoading,
      mergeHasNoConflict,
      targetBranchDeleted,
      rejectButtonLoading,
      showNotification,
      subscriptionIcon,
      subscriptionLabel,
      subscriptionLink,
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
          <MediaContent>
            <MarkdownView className="content" content={pullRequest.description} />
          </MediaContent>
        </div>
      );
    }

    let mergeNotification = null;
    if (showNotification) {
      mergeNotification = (
        <Notification
          type="info"
          children={t("scm-review-plugin.show-pull-request.notification")}
          onClose={() => this.onClose()}
        />
      );
    }

    let mergeButton = null;
    let rejectButton = null;
    if (pullRequest._links.reject) {
      rejectButton = <RejectButton reject={() => this.performReject()} loading={rejectButtonLoading} />;
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
        "/repo/" + repository.namespace + "/" + repository.name + "/pull-request/" + pullRequest.id + "/edit";
      editButton = (
        <div className="media-right">
          <Button
            link={toEdit}
            color="link is-outlined"
            label={t("scm-review-plugin.edit.button")}
            icon="edit"
            reducedMobile={true}
          />
        </div>
      );
    }

    const subscription = subscriptionLink ? (
      <div className="level-left">
        <div className="level-item">
          <Button action={this.handleSubscription} loading={loadingSubscription} color="link is-outlined">
            <span className="icon is-small">
              <i className={`fas fa-${subscriptionIcon}`} />
            </span>
            <span>{t("scm-review-plugin.edit." + subscriptionLabel)}</span>
          </Button>
        </div>
      </div>
    ) : (
      ""
    );
    const targetBranchDeletedWarning = targetBranchDeleted ? (
      <Tooltip className="icon has-text-warning" message={t("scm-review-plugin.show-pull-request.targetDeleted")}>
        <i className="fas fa-exclamation-triangle" />
      </Tooltip>
    ) : null;

    const author = (
      <div className="field is-horizontal">
        <UserLabel>{t("scm-review-plugin.pull-request.author")}:</UserLabel>
        <UserField>
          <UserInline>{pullRequest.author.displayName}</UserInline>
          <DateFromNow date={pullRequest.creationDate} />
        </UserField>
      </div>
    );
    const reviewerList = (
      <>
        {pullRequest.reviewer.length > 0 ? (
          <div className="field is-horizontal">
            <UserLabel>{t("scm-review-plugin.pull-request.reviewer")}:</UserLabel>
            <UserField>
              <ul className="is-separated">
                {pullRequest.reviewer.map(reviewer => {
                  return <UserInlineListItem key={reviewer.id}>{reviewer.displayName}</UserInlineListItem>;
                })}
              </ul>
            </UserField>
          </div>
        ) : (
          ""
        )}
      </>
    );
    return (
      <>
        <Container>
          <div className="media">
            <div className="media-content">
              <Title title={" #" + pullRequest.id + " " + pullRequest.title} />
            </div>
            {editButton}
          </div>

          {mergeNotification}

          <MediaWithTopBorder>
            <div className="media-content">
              <ShortTag label={pullRequest.source} title={pullRequest.source} />{" "}
              <i className="fas fa-long-arrow-alt-right" />{" "}
              <ShortTag label={pullRequest.target} title={pullRequest.target} />
              {targetBranchDeletedWarning}
            </div>
            <div className="media-right">
              <Tag
                className="is-medium"
                color={
                  pullRequest.status === "MERGED" ? "success" : pullRequest.status === "REJECTED" ? "danger" : "light"
                }
                label={pullRequest.status}
              />
            </div>
          </MediaWithTopBorder>
          <ExtensionPoint
            name={"reviewPlugin.pullrequest.top"}
            renderAll={true}
            props={{
              repository,
              pullRequest
            }}
          />
          {description}

          <UserList className="media">
            <div className="media-content">
              {author}
              {reviewerList}
            </div>
          </UserList>

          <LevelWrapper className="level">
            {subscription}
            <div className="level-right">
              <div className="level-item">{rejectButton}</div>
              <div className="level-item">{mergeButton}</div>
            </div>
          </LevelWrapper>
        </Container>

        <PullRequestInformation
          pullRequest={pullRequest}
          baseURL={match.url}
          repository={repository}
          source={pullRequest.source}
          target={pullRequest.target}
          status={pullRequest.status}
        />
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestDetails));
