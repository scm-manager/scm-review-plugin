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
import styled from "styled-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { Link, Repository } from "@scm-manager/ui-types";
import { binder, ExtensionPoint } from "@scm-manager/ui-extensions";
import {
  AstPlugin,
  Button,
  ButtonGroup,
  ConflictError,
  DateFromNow,
  ErrorNotification,
  Icon,
  Loading,
  NotFoundError,
  Tag,
  Title,
  Tooltip
} from "@scm-manager/ui-components";
import { MergeCheck, MergeCommit, PullRequest } from "./types/PullRequest";
import { check, evaluateTagColor, merge, reject } from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
import ApprovalContainer from "./ApprovalContainer";
import SubscriptionContainer from "./SubscriptionContainer";
import ReviewerList from "./ReviewerList";
import ChangeNotification from "./ChangeNotification";
import ReducedMarkdownView from "./ReducedMarkdownView";
import Statusbar from "./workflow/Statusbar";
import OverrideModalRow from "./OverrideModalRow";
import PullRequestTitle from "./PullRequestTitle";

type Props = WithTranslation &
  RouteComponentProps & {
    repository: Repository;
    pullRequest: PullRequest;
  };

type State = {
  error?: Error;
  loadingDryRun: boolean;
  mergeCheck?: MergeCheck;
  targetBranchDeleted: boolean;
  mergeButtonLoading: boolean;
  rejectButtonLoading: boolean;
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

const RightMarginTitle = styled(Title)`
  margin-right: 0.5rem !important;
`;

const TitleTag = styled(Tag).attrs((props: any) => ({
  className: "is-medium",
  color: props.color
}))`
  margin-top: 0.25rem;
`;

const MobileFlexButtonGroup = styled(ButtonGroup)`
  @media screen and (max-width: 768px) {
    flex-direction: column;

    > .control:not(:last-child) {
      margin-right: 0 !important;
      margin-bottom: 0.75rem !important;
    }
  }
`;

const UserList = styled.div`
  margin-bottom: 1rem;
`;

const LevelWrapper = styled.div`
  flex-flow: row wrap;

  & > * {
    margin-top: 0.5rem;
  }

  & > .level-right {
    margin-left: auto;
  }
`;

const IgnoredMergeObstacles = styled.div`
  padding: 1rem 0;
  margin: 1rem 0;
  border-bottom: 1px solid hsla(0, 0%, 85.9%, 0.5);
`;

class PullRequestDetails extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      ...this.state,
      loadingDryRun: false,
      mergeButtonLoading: true,
      rejectButtonLoading: false,
      targetBranchDeleted: false
    };
  }

  componentDidMount(): void {
    const { pullRequest } = this.props;
    this.getMergeDryRun(pullRequest);
  }

  shouldRunDryMerge = (pullRequest: PullRequest) => {
    return (
      pullRequest._links.mergeCheck && (pullRequest._links.mergeCheck as Link).href && pullRequest.status === "OPEN"
    );
  };

  onMergeModalClosed = () => {
    this.setState({
      mergeCheck: undefined
    });
  };

  getMergeDryRun(pullRequest: PullRequest) {
    if (this.shouldRunDryMerge(pullRequest)) {
      check(pullRequest)
        .then(response => {
          this.setState({
            mergeCheck: response,
            targetBranchDeleted: false,
            loadingDryRun: false,
            mergeButtonLoading: false
          });
        })
        .catch(err => {
          if (err instanceof NotFoundError) {
            this.setState({
              mergeButtonLoading: false,
              loadingDryRun: false,
              targetBranchDeleted: true
            });
          } else {
            this.setState({
              error: err,
              loadingDryRun: false,
              mergeButtonLoading: false
            });
          }
        });
    }
  }

  findStrategyLink = (links: Link[], strategy: string) => {
    return links?.filter(link => link.name === strategy)[0].href;
  };

  performMerge = (strategy: string, commit: MergeCommit, emergency: boolean) => {
    const { pullRequest, fetchPullRequest } = this.props;
    const mergeLinks = emergency
      ? (pullRequest?._links?.emergencyMerge as Link[])
      : (pullRequest?._links?.merge as Link[]);

    this.setMergeButtonLoadingState();
    merge(this.findStrategyLink(mergeLinks, strategy), commit)
      .then(fetchPullRequest)
      .catch(err => {
        if (err instanceof ConflictError) {
          this.setState({
            mergeCheck: {
              mergeObstacles: this.state.mergeCheck ? this.state.mergeCheck.mergeObstacles : [],
              hasConflicts: true
            },
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

  render() {
    const { repository, pullRequest, match, t } = this.props;
    const {
      error,
      loadingDryRun,
      mergeButtonLoading,
      mergeCheck,
      targetBranchDeleted,
      rejectButtonLoading
    } = this.state;

    if (error) {
      return <ErrorNotification error={error} />;
    }

    if (!pullRequest || loadingDryRun) {
      return <Loading />;
    }

    let description = null;
    if (pullRequest.description) {
      description = (
        <div className="media">
          <MediaContent>
            <ReducedMarkdownView
              content={pullRequest.description}
              plugins={binder
                .getExtensions("pullrequest.description.plugins", {
                  halObject: pullRequest
                })
                .map(pluginFactory => pluginFactory({ halObject: pullRequest }) as AstPlugin)}
            />
          </MediaContent>
        </div>
      );
    }

    let ignoredMergeObstacles = null;
    if (pullRequest.ignoredMergeObstacles?.length > 0) {
      ignoredMergeObstacles = (
        <IgnoredMergeObstacles>
          <strong>{t("scm-review-plugin.pullRequest.details.ignoredMergeObstacles")}</strong>
          {pullRequest.ignoredMergeObstacles.map(o => (
            <OverrideModalRow result={{ rule: o, failed: true }} useObstacleText={pullRequest.emergencyMerged} />
          ))}
        </IgnoredMergeObstacles>
      );
    }

    let mergeButton = null;
    let rejectButton = null;
    if (pullRequest._links.reject) {
      rejectButton = <RejectButton reject={() => this.performReject()} loading={rejectButtonLoading} />;
      if (!!pullRequest._links.merge) {
        mergeButton = targetBranchDeleted ? null : (
          <MergeButton
            merge={(strategy: string, commit: MergeCommit, emergency) => this.performMerge(strategy, commit, emergency)}
            mergeCheck={mergeCheck}
            loading={mergeButtonLoading}
            repository={repository}
            pullRequest={pullRequest}
            onMergeModalClose={this.onMergeModalClosed}
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
          <Icon name="edit fa-fw" color="inherit" />
        </Button>
      );
    }

    let subscriptionButton = null;
    if (pullRequest._links.subscription && (pullRequest._links.subscription as Link).href) {
      subscriptionButton = <SubscriptionContainer pullRequest={pullRequest} />;
    }

    const targetBranchDeletedWarning = targetBranchDeleted ? (
      <Tooltip className="icon has-text-warning" message={t("scm-review-plugin.pullRequest.details.targetDeleted")}>
        <i className="fas fa-exclamation-triangle" />
      </Tooltip>
    ) : null;

    const userEntry = (labelKey: string, displayName: string, date?: string) => {
      return (
        <div className="field is-horizontal">
          <UserLabel>{t("scm-review-plugin.pullRequest." + labelKey)}:</UserLabel>
          <UserField>
            <UserInline>{displayName}</UserInline>
            &nbsp;
            {date ? <DateFromNow date={date} /> : null}
          </UserField>
        </div>
      );
    };

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
        <ChangeNotification repository={repository} pullRequest={pullRequest} reload={this.props.fetchPullRequest} />
        <Container>
          <div className="media">
            <div className="media-content">
              <RightMarginTitle className="is-inline is-marginless">
                #{pullRequest.id} <PullRequestTitle pullRequest={pullRequest} />
              </RightMarginTitle>
              {totalTasks > 0 && (
                <TitleTag
                  label={titleTagText}
                  title={titleTagText}
                  color={pullRequest.tasks.done < totalTasks ? "light" : "success"}
                />
              )}
            </div>
            <div className="media-right">
              <MobileFlexButtonGroup>
                {subscriptionButton}
                {editButton}
              </MobileFlexButtonGroup>
            </div>
          </div>
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
                color={evaluateTagColor(pullRequest)}
                label={t("scm-review-plugin.pullRequest.statusLabel." + pullRequest.status)}
                icon={pullRequest.emergencyMerged ? "exclamation-triangle" : undefined}
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
          <Statusbar pullRequest={pullRequest} />
          {description}
          {ignoredMergeObstacles}
          <UserList className="media">
            <div className="media-content">
              <ExtensionPoint
                name="reviewPlugin.pullrequest.userList"
                renderAll={true}
                props={{
                  repository,
                  pullRequest
                }}
              />
              {userEntry("author", pullRequest.author.displayName, pullRequest.creationDate)}
              {pullRequest.status !== "OPEN" && !!pullRequest.reviser?.displayName
                ? userEntry(
                    pullRequest.status === "MERGED" ? "mergedBy" : "rejectedBy",
                    pullRequest.reviser.displayName,
                    pullRequest.closeDate
                  )
                : null}
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

        <ExtensionPoint
          name="reviewPlugin.pullrequest.bottom"
          renderAll={true}
          props={{
            repository,
            pullRequest
          }}
        />

        <PullRequestInformation
          pullRequest={pullRequest}
          baseURL={match.url}
          repository={repository}
          source={pullRequest.source}
          target={pullRequest.target}
          status={pullRequest.status}
          mergeHasNoConflict={!mergeCheck?.hasConflicts}
          targetBranchDeleted={targetBranchDeleted}
        />
      </>
    );
  }
}

export default withRouter(withTranslation("plugins")(PullRequestDetails));
