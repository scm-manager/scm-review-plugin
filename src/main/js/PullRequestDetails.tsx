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
  Tooltip
} from "@scm-manager/ui-components";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { dryRun, merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
import ApprovalContainer from "./ApprovalContainer";
import SubscriptionContainer from "./SubscriptionContainer";
import ReviewerList from "./ReviewerList";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
    fetchReviewer: () => void;
  };

type State = {
  error?: Error;
  loading: boolean;
  mergeHasNoConflict?: boolean;
  targetBranchDeleted?: boolean;
  mergeButtonLoading: boolean;
  rejectButtonLoading: boolean;
  showNotification: boolean;
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
      ...this.state,
      loading: false,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      showNotification: false,
      mergeHasNoConflict: false
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

  shouldRunDryMerge = (pullRequest: PullRequest) => {
    return (
      pullRequest._links.mergeDryRun && (pullRequest._links.mergeDryRun as Link).href && pullRequest.status === "OPEN"
    );
  };

  getMergeDryRun(pullRequest: PullRequest) {
    if (this.shouldRunDryMerge(pullRequest)) {
      dryRun(pullRequest).then(response => {
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

  findStrategyLink = (links: Link[], strategy: string) => {
    return links.filter(link => link.name === strategy)[0].href;
  };

  performMerge = (strategy: string, commit: MergeCommit) => {
    const { pullRequest } = this.props;
    this.setMergeButtonLoadingState();
    merge(this.findStrategyLink(pullRequest._links.merge as Link[], strategy), commit).then(response => {
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
    const { pullRequest } = this.props;
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
    return (
      <>
        <Container>
          <div className="media">
            <div className="media-content">
              <Title title={" #" + pullRequest.id + " " + pullRequest.title} />
            </div>
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
        />
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestDetails));
