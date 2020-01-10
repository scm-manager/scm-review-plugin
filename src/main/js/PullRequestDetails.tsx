import React from "react";
import styled from "styled-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { Link, Repository } from "@scm-manager/ui-types";
import { ExtensionPoint } from "@scm-manager/ui-extensions";
import {
  Button,
  ButtonGroup,
  DateFromNow,
  ErrorNotification,
  Icon,
  Loading,
  MarkdownView,
  Notification,
  Tag,
  Title,
  Tooltip,
  ConflictError,
  NotFoundError
} from "@scm-manager/ui-components";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { dryRun, merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
import ApprovalContainer from "./ApprovalContainer";
import SubscriptionContainer from "./SubscriptionContainer";
import ReviewerList from "./ReviewerList";
import ChangeNotification from "./ChangeNotification";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    fetchReviewer: () => void;
    fetchPullRequest: () => void;
  };

type State = {
  error?: Error;
  loading: boolean;
  mergeHasNoConflict?: boolean;
  targetBranchDeleted: boolean;
  mergeButtonLoading: boolean;
  rejectButtonLoading: boolean;
  showNotification: boolean;
};

const MediaContent = styled.div.attrs(() => ({
  className: "media-content"
}))`
  width: 100%;
  word-wrap: break-word;
`;

const UserLabel = styled.div.attrs(() => ({
  className: "field-label is-inline-flex"
}))`
  text-align: left;
  margin-right: 0;
  min-width: 5.5em;
`;

const UserField = styled.div.attrs(() => ({
  className: "field-body is-inline-flex"
}))`
  flex-grow: 8;
`;

const UserInline = styled.div`
  display: inline-block;
  font-weight: bold;
`;

const Container = styled.div`
  margin-bottom: 2rem;
  padding: 1rem;
  border: 1px solid #dbdbdb; // border
  border-radius: 4px;
`;

const MediaWithTopBorder = styled.div.attrs(() => ({
  className: "media"
}))`
  padding: 0 !important;
  border-top: none !important;
`;

const ShortTag = styled(Tag).attrs(() => ({
  className: "is-medium",
  color: "light"
}))`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 25em;
`;

const TitleTag = styled(Tag).attrs((props: any) => ({
  className: "is-medium",
  color: props.color
}))`
  margin-left: 1em;
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
      ...this.state,
      loading: true,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false,
      targetBranchDeleted: false,
      mergeHasNoConflict: true,
      subscriptionIcon: "",
      subscriptionLabel: "",
      subscriptionLink: ""
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    this.getMergeDryRun(pullRequest);
  }

  shouldRunDryMerge = (pullRequest: PullRequest) => {
    return (
      pullRequest._links.mergeDryRun && (pullRequest._links.mergeDryRun as Link).href && pullRequest.status === "OPEN"
    );
  };

  getMergeDryRun(pullRequest: PullRequest) {
    if (this.shouldRunDryMerge(pullRequest)) {
      dryRun(pullRequest)
        .then(response => {
          this.setState({
            mergeHasNoConflict: true,
            targetBranchDeleted: false,
            loading: false,
            mergeButtonLoading: false
          });
        })
        .catch(err => {
          if (err instanceof ConflictError) {
            this.setState({
              mergeButtonLoading: false,
              loading: false,
              mergeHasNoConflict: false
            });
          } else if (err instanceof NotFoundError) {
            this.setState({
              mergeButtonLoading: false,
              loading: false,
              targetBranchDeleted: true
            });
          } else {
            this.setState({
              error: err,
              loading: false,
              mergeButtonLoading: false
            });
          }
        });
    }
  }

  findStrategyLink = (links: Link[], strategy: string) => {
    return links.filter(link => link.name === strategy)[0].href;
  };

  performMerge = (strategy: string, commit: MergeCommit) => {
    const { pullRequest, fetchPullRequest } = this.props;
    this.setMergeButtonLoadingState();
    merge(this.findStrategyLink(pullRequest._links.merge as Link[], strategy), commit)
      .then(response => {
        this.setState({
          loading: true,
          showNotification: true,
          mergeButtonLoading: false
        });
        fetchPullRequest();
      })
      .catch(err => {
        if (err instanceof ConflictError) {
          this.setState({
            mergeHasNoConflict: false,
            mergeButtonLoading: false
          });
        } else {
          this.setState({
            error: err,
            mergeButtonLoading: false
          });
        }
      });
  };

  performReject = () => {
    this.setState({
      rejectButtonLoading: true
    });
    const { pullRequest, fetchPullRequest } = this.props;
    reject(pullRequest)
      .then(() => {
        this.setState({
          rejectButtonLoading: false
        });
        fetchPullRequest();
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
      mergeHasNoConflict: false
    });
  };

  render() {
    const { repository, pullRequest, match, t } = this.props;
    const {
      error,
      loading,
      mergeButtonLoading,
      mergeHasNoConflict,
      targetBranchDeleted,
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
          children={t("scm-review-plugin.pullRequest.details.notification")}
          onClose={() => this.onClose()}
        />
      );
    }

    let mergeButton = null;
    let rejectButton = null;
    if (pullRequest._links.reject) {
      rejectButton = <RejectButton reject={() => this.performReject()} loading={rejectButtonLoading} />;
      if (!!pullRequest._links.merge) {
        mergeButton = targetBranchDeleted ? null : (
          <MergeButton
            merge={(strategy: string, commit: MergeCommit) => this.performMerge(strategy, commit)}
            mergeHasNoConflict={mergeHasNoConflict}
            loading={mergeButtonLoading}
            repository={repository}
            pullRequest={pullRequest}
          />
        );
      }
    }

    let editButton = null;
    if (pullRequest._links.update && (pullRequest._links.update as Link).href) {
      const toEdit =
        "/repo/" + repository.namespace + "/" + repository.name + "/pull-request/" + pullRequest.id + "/edit";
      editButton = (
        <Button link={toEdit} title={t("scm-review-plugin.pullRequest.details.buttons.edit")} color="link is-outlined">
          <Icon name="edit" color="inherit" />
        </Button>
      );
    }

    const targetBranchDeletedWarning = targetBranchDeleted ? (
      <Tooltip className="icon has-text-warning" message={t("scm-review-plugin.pullRequest.details.targetDeleted")}>
        <i className="fas fa-exclamation-triangle" />
      </Tooltip>
    ) : null;

    const author = (
      <div className="field is-horizontal">
        <UserLabel>{t("scm-review-plugin.pullRequest.author")}:</UserLabel>
        <UserField>
          <UserInline>{pullRequest.author.displayName}</UserInline>
          &nbsp;
          <DateFromNow date={pullRequest.creationDate} />
        </UserField>
      </div>
    );

    const totalTasks = pullRequest.tasks.todo + pullRequest.tasks.done;

    const titleTagText =
      pullRequest.tasks.done < totalTasks
        ? t("scm-review-plugin.pullRequest.tasks.done", {
            done: pullRequest.tasks.done,
            total: totalTasks
          })
        : t("scm-review-plugin.pullRequest.tasks.allDone");

    return (
      <>
        <ChangeNotification pullRequest={pullRequest} reload={this.props.fetchPullRequest}/>
        <Container>
          <div className="media">
            <UserField className="media-content">
              <Title title={"#" + pullRequest.id + " " + pullRequest.title} />
              {totalTasks > 0 && (
                <TitleTag
                  label={titleTagText}
                  title={titleTagText}
                  color={pullRequest.tasks.done < totalTasks ? "light" : "success"}
                />
              )}
            </UserField>
            <div className="media-right">
              <ButtonGroup>
                <SubscriptionContainer pullRequest={pullRequest} />
                {editButton}
              </ButtonGroup>
            </div>
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
            name="reviewPlugin.pullrequest.top"
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
              <ReviewerList pullRequest={pullRequest} reviewer={pullRequest.reviewer} />
            </div>
          </UserList>

          <LevelWrapper className="level">
            <div className="level-left">
              <div className="level-item">
                <ApprovalContainer pullRequest={pullRequest} refreshReviewer={() => this.props.fetchReviewer()} />
              </div>
            </div>
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
          mergeHasNoConflict={mergeHasNoConflict}
          targetBranchDeleted={targetBranchDeleted}
        />
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestDetails));
