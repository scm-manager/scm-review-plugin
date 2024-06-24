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
import React, { FC, useCallback, useEffect, useMemo, useState } from "react";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { Link, Repository } from "@scm-manager/ui-types";
import { binder, ExtensionPoint } from "@scm-manager/ui-extensions";
import { useBranch } from "@scm-manager/ui-api";
import {
  AstPlugin,
  BackendError,
  Button,
  ButtonGroup,
  DateFromNow,
  devices,
  ErrorNotification,
  Icon,
  Loading,
  Tag,
  Tooltip
} from "@scm-manager/ui-components";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import {
  invalidateQueries,
  prQueryKey,
  prsQueryKey,
  useMergeDryRun,
  useMergePullRequest,
  useReadyForReviewPullRequest,
  useRejectPullRequest,
  useReopenPullRequest
} from "./pullRequest";
import PullRequestInformation from "./PullRequestInformation";
import MergeButton from "./MergeButton";
import RejectButton from "./RejectButton";
import ApprovalContainer from "./ApprovalContainer";
import SubscriptionContainer from "./SubscriptionContainer";
import ReviewerList from "./ReviewerList";
import ChangeNotification from "./ChangeNotification";
import ReducedMarkdownView from "./ReducedMarkdownView";
import OverrideModalRow from "./OverrideModalRow";
import PullRequestTitle from "./PullRequestTitle";
import Statusbar from "./workflow/Statusbar";
import PullRequestStatusTag from "./PullRequestStatusTag";
import ChangeNotificationContext from "./ChangeNotificationContext";
import SourceTargetBranchDisplay from "./SourceTargetBranchDisplay";
import DeleteSourceBranchButton from "./DeleteSourceBranchButton";
import LabelsList from "./LabelsList";
import ReopenButton from "./ReopenButton";
import { useQueryClient } from "react-query";
import ResizeObserver from "resize-observer-polyfill";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
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

const MobileFlexButtonGroup = styled(ButtonGroup)`
  @media screen and (max-width: 768px) {
    flex-direction: column;

    > .control:not(:last-child) {
      margin-right: 0 !important;
      margin-bottom: 0.75rem !important;
    }
  }
`;

const StyledHeader = styled.h2`
  word-break: break-all;
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
  border-bottom: 1px solid hsla(0, 0%, 85.9%, 0.5);
`;

const StickyHeader = styled.div`
  position: sticky;
  display: flex;
  top: var(--scm-navbar-main-height);
  justify-content: space-between;
  min-height: 50px;
  color: var(--scm-panel-heading-color);
  background: var(--scm-panel-heading-background-color);
  border: var(--scm-border);
  border-top: 0;
  border-radius: 0 0 0.25rem 0.25rem;
  z-index: 10;
  animation: fadeIn 200ms alternate;
  transform-origin: center top;

  @keyframes fadeIn {
    0% {
      top: 0;
    }
    100% {
      top: var(--scm-navbar-main-height);
    }
  }
`;

const LeftSide = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem 1.25rem;
  margin: 0.5em 0.75em;
  padding: 0 0.75rem;
  width: calc(100% - 140px);

  @media screen and (max-width: ${devices.mobile.width}px) {
    width: 100%;
  }
`;

type UserEntryProps = {
  labelKey: string;
  displayName: string;
  date?: string;
};

const UserEntry: FC<UserEntryProps> = ({ labelKey, displayName, date }) => {
  const [t] = useTranslation("plugins");
  return (
    <div className="field is-horizontal">
      <UserLabel>{t("scm-review-plugin.pullRequest." + labelKey)}:</UserLabel>
      <UserField>
        <div className="is-inline-block has-text-weight-bold">{displayName}</div>
        &nbsp;
        {date ? <DateFromNow date={date} /> : null}
      </UserField>
    </div>
  );
};

const PullRequestDetails: FC<Props> = ({ repository, pullRequest }) => {
  const [t] = useTranslation("plugins");
  const [targetOrSourceBranchDeleted, setTargetOrSourceBranchDeleted] = useState(false);
  const [isVisible, setVisible] = useState(false);

  const { reject, isLoading: rejectLoading, error: rejectError } = useRejectPullRequest(repository, pullRequest);
  const { reopen, isLoading: reopenLoading, error: reopenError } = useReopenPullRequest(repository, pullRequest);
  const { readyForReview, isLoading: readyForReviewLoading, error: readyForReviewError } = useReadyForReviewPullRequest(
    repository,
    pullRequest
  );
  const { merge, isLoading: mergeLoading, error: mergeError } = useMergePullRequest(repository, pullRequest);
  const { data: mergeCheck, isLoading: mergeDryRunLoading, error: mergeDryRunError } = useMergeDryRun(
    repository,
    pullRequest,
    (targetOrSourceDeleted: boolean) => setTargetOrSourceBranchDeleted(targetOrSourceDeleted)
  );
  const { isLoading: branchLoading, data: branch } = useBranch(repository, pullRequest.source);
  const astPlugins = useMemo(() => {
    if (!!pullRequest._links) {
      return binder
        .getExtensions("pullrequest.description.plugins", {
          halObject: pullRequest
        })
        .map(pluginFactory => pluginFactory({ halObject: pullRequest }) as AstPlugin);
    }
    return [];
  }, [pullRequest]);
  const queryClient = useQueryClient();
  useEffect(() => {
    invalidateQueries(queryClient, prsQueryKey(repository), prQueryKey(repository, pullRequest.id!));
  }, [queryClient, pullRequest.id, repository, branch]);

  const [stickyHeaderHeight, setStickyHeaderHeight] = useState(0);

  const enableHeaderRef = useCallback(node => {
    if (node !== null) {
      const intersectionObserver = new IntersectionObserver(entries => {
        const entry = entries[0];

        const isTargetNoLongerInViewport = () => {
          return !entry?.isIntersecting && entry?.boundingClientRect.top < 0;
        };

        setVisible(isTargetNoLongerInViewport());
      });
      intersectionObserver.observe(node);
    }
  }, []);

  const contentHeightRef = useCallback(node => {
    if (node !== null) {
      const resizeObserver = new ResizeObserver(entries => {
        const entry = entries[0];

        setStickyHeaderHeight(entry.contentRect.height);
      });
      resizeObserver.observe(node);
    }
  }, []);

  const findStrategyLink = (links: Link[], strategy: string) => {
    return links?.filter(link => link.name === strategy)[0].href;
  };

  const performMerge = (strategy: string, commit: MergeCommit, emergency: boolean) => {
    const mergeLinks = emergency
      ? (pullRequest?._links?.emergencyMerge as Link[])
      : (pullRequest?._links?.merge as Link[]);

    merge({ url: findStrategyLink(mergeLinks, strategy), mergeCommit: commit });
  };

  const error =
    rejectError ??
    reopenError ??
    mergeError ??
    readyForReviewError ??
    // Prevent dry-run error for closed pull request with stale cache data
    ((mergeDryRunError instanceof BackendError && mergeDryRunError.errorCode === "FTRhcI0To1") ||
    targetOrSourceBranchDeleted
      ? null
      : mergeDryRunError);
  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!pullRequest._links || mergeDryRunLoading || branchLoading) {
    return <Loading />;
  }

  let description = null;
  if (pullRequest.description) {
    description = (
      <div className="media">
        <MediaContent>
          <ReducedMarkdownView content={pullRequest.description} plugins={astPlugins} />
        </MediaContent>
      </div>
    );
  }

  let ignoredMergeObstacles = null;
  const ignoredMergeObstaclesArray = pullRequest.ignoredMergeObstacles || [];
  if (ignoredMergeObstaclesArray.length || -1 > 0) {
    ignoredMergeObstacles = (
      <IgnoredMergeObstacles className="mx-0 my-4 px-0 py-4">
        <strong>{t("scm-review-plugin.pullRequest.details.ignoredMergeObstacles")}</strong>
        {ignoredMergeObstaclesArray.map(o => (
          <OverrideModalRow key={o} result={{ rule: o, failed: true }} />
        ))}
      </IgnoredMergeObstacles>
    );
  }

  let mergeButton = null;
  let rejectButton = null;
  let reopenButton = null;
  let deleteSourceButton = null;
  let readyForReviewButton = null;

  if (pullRequest.status === "REJECTED" && pullRequest._links?.reopen && !targetOrSourceBranchDeleted) {
    reopenButton = <ReopenButton reopen={reopen} loading={reopenLoading} />;
  }

  if (pullRequest._links?.rejectWithMessage) {
    rejectButton = <RejectButton reject={message => reject(message)} loading={rejectLoading} />;
    if (!!pullRequest._links.merge) {
      mergeButton = targetOrSourceBranchDeleted ? null : (
        <MergeButton
          merge={(strategy: string, commit: MergeCommit, emergency) => performMerge(strategy, commit, emergency)}
          mergeCheck={mergeCheck}
          loading={mergeLoading}
          repository={repository}
          pullRequest={pullRequest}
        />
      );
    }
  }
  if ((pullRequest.status === "MERGED" || pullRequest.status === "REJECTED") && branch?._links?.delete) {
    deleteSourceButton = (
      <DeleteSourceBranchButton pullRequest={pullRequest} repository={repository} loading={mergeDryRunLoading} />
    );
  }
  if (pullRequest._links?.convertToPR) {
    readyForReviewButton = (
      <Button
        label={t("scm-review-plugin.showPullRequest.convertDraftButton.buttonTitle")}
        action={() => readyForReview(pullRequest)}
        loading={readyForReviewLoading}
        color="primary"
      />
    );
  }

  let editButton = null;
  if ((pullRequest._links?.update as Link)?.href) {
    const toEdit = `/repo/${repository.namespace}/${repository.name}/pull-request/${pullRequest.id}/edit`;
    editButton = (
      <Button link={toEdit} title={t("scm-review-plugin.pullRequest.details.buttons.edit")} color="link is-outlined">
        <Icon name="edit fa-fw" color="inherit" />
      </Button>
    );
  }

  let subscriptionButton = null;
  if ((pullRequest._links?.subscription as Link)?.href) {
    subscriptionButton = <SubscriptionContainer repository={repository} pullRequest={pullRequest} />;
  }

  const targetBranchDeletedWarning = targetOrSourceBranchDeleted ? (
    <span className="ml-2">
      <Tooltip className="icon has-text-warning" message={t("scm-review-plugin.pullRequest.details.branchDeleted")}>
        <i className="fas fa-exclamation-triangle" />
      </Tooltip>
    </span>
  ) : null;

  const detailStickyHeader = isVisible ? (
    <StickyHeader ref={contentHeightRef}>
      <LeftSide>
        <strong>
          #{pullRequest.id} <PullRequestTitle pullRequest={pullRequest} />
        </strong>
        <div>
          {pullRequest.source}
          <i className="fas fa-long-arrow-alt-right m-1" />
          {pullRequest.target}
          {targetBranchDeletedWarning}
        </div>
      </LeftSide>
    </StickyHeader>
  ) : null;

  const tasksDone = pullRequest.tasks ? pullRequest.tasks.done : 0;
  const totalTasks = pullRequest.tasks ? pullRequest.tasks.done + pullRequest.tasks.todo : 0;

  const titleTagText =
    tasksDone < totalTasks
      ? t("scm-review-plugin.pullRequest.tasks.done", {
          done: tasksDone,
          total: totalTasks
        })
      : t("scm-review-plugin.pullRequest.tasks.allDone");

  const getLabelKeyForUser = () => {
    return pullRequest.status === "MERGED" ? "mergedBy" : "rejectedBy";
  };

  return (
    <ChangeNotificationContext>
      <ChangeNotification repository={repository} pullRequest={pullRequest} />
      {detailStickyHeader}

      <Container>
        <div className="media">
          <div className="media-content">
            <StyledHeader className="is-inline has-text-weight-medium is-size-3 mr-2">
              #{pullRequest.id} <PullRequestTitle pullRequest={pullRequest} />
            </StyledHeader>
            {totalTasks > 0 && (
              <Tag
                className="is-medium mt-1"
                label={titleTagText}
                title={titleTagText}
                color={tasksDone < totalTasks ? "light" : "success"}
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
          <SourceTargetBranchDisplay source={pullRequest.source} target={pullRequest.target} className="media-content">
            {targetBranchDeletedWarning}
          </SourceTargetBranchDisplay>
          <div className="media-right">
            <PullRequestStatusTag status={pullRequest.status || "OPEN"} emergencyMerged={pullRequest.emergencyMerged} />
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
        <Statusbar repository={repository} pullRequest={pullRequest} />
        {description}
        {ignoredMergeObstacles}
        <div className="media mb-4" ref={enableHeaderRef}>
          <div className="media-content">
            <ExtensionPoint
              name="reviewPlugin.pullrequest.userList"
              renderAll={true}
              props={{
                repository,
                pullRequest
              }}
            />
            {pullRequest.author ? (
              <UserEntry
                labelKey="author"
                displayName={pullRequest.author.displayName}
                date={pullRequest.creationDate}
              />
            ) : null}
            {pullRequest.status !== "OPEN" && !!pullRequest.reviser?.displayName ? (
              <UserEntry
                labelKey={getLabelKeyForUser()}
                displayName={pullRequest.reviser?.displayName}
                date={pullRequest.closeDate}
              />
            ) : null}
            <ReviewerList pullRequest={pullRequest} />
            <LabelsList labels={pullRequest.labels} />
          </div>
        </div>

        <LevelWrapper className="level">
          <div className="level-left">
            <div className="level-item">
              <ApprovalContainer repository={repository} pullRequest={pullRequest} />
            </div>
          </div>
          <div className="level-right">
            <div className="buttons">
              {rejectButton}
              {reopenButton}
              {mergeButton}
              {deleteSourceButton}
              {readyForReviewButton}
            </div>
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
        repository={repository}
        pullRequest={pullRequest}
        source={pullRequest.source}
        target={pullRequest.target}
        status={pullRequest.status}
        mergeHasNoConflict={!mergeCheck?.hasConflicts}
        mergePreventReasons={mergeCheck?.mergePreventReasons ?? []}
        targetBranchDeleted={targetOrSourceBranchDeleted}
        sourceBranch={branch}
        stickyHeaderHeight={stickyHeaderHeight}
      />
    </ChangeNotificationContext>
  );
};

export default PullRequestDetails;
