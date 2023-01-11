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
import React, { FC, useMemo, useState } from "react";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import { Link, Repository } from "@scm-manager/ui-types";
import { binder, ExtensionPoint } from "@scm-manager/ui-extensions";
import {
  AstPlugin,
  BackendError,
  Button,
  ButtonGroup,
  DateFromNow,
  ErrorNotification,
  Icon,
  Loading,
  Tag,
  Tooltip
} from "@scm-manager/ui-components";
import { MergeCommit, PullRequest } from "./types/PullRequest";
import { useMergeDryRun, useMergePullRequest, useRejectPullRequest } from "./pullRequest";
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
  const [targetBranchDeleted, setTargetBranchDeleted] = useState(false);

  const { reject, isLoading: rejectLoading, error: rejectError } = useRejectPullRequest(repository, pullRequest);
  const { merge, isLoading: mergeLoading, error: mergeError } = useMergePullRequest(repository, pullRequest);
  const { data: mergeCheck, isLoading: mergeDryRunLoading, error: mergeDryRunError } = useMergeDryRun(
    repository,
    pullRequest,
    (targetDeleted: boolean) => setTargetBranchDeleted(targetDeleted)
  );
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
    rejectError ||
    mergeError ||
    // Prevent dry-run error for closed pull request with stale cache data
    (mergeDryRunError instanceof BackendError && mergeDryRunError.errorCode === "FTRhcI0To1" ? null : mergeDryRunError);
  if (error) {
    return <ErrorNotification error={error} />;
  }

  if (!pullRequest._links || mergeDryRunLoading) {
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
  let deleteSourceButton = null;
  if (pullRequest._links?.reject) {
    rejectButton = <RejectButton reject={() => reject(pullRequest)} loading={rejectLoading} />;
    if (!!pullRequest._links.merge) {
      mergeButton = targetBranchDeleted ? null : (
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
  if (!pullRequest._links.reject && !pullRequest._links.merge) {
    deleteSourceButton = (
      <DeleteSourceBranchButton pullRequest={pullRequest} repository={repository} loading={mergeDryRunLoading} />
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

  const targetBranchDeletedWarning = targetBranchDeleted ? (
    <span className="ml-2">
      <Tooltip className="icon has-text-warning" message={t("scm-review-plugin.pullRequest.details.targetDeleted")}>
        <i className="fas fa-exclamation-triangle" />
      </Tooltip>
    </span>
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
      <Container>
        <div className="media">
          <div className="media-content">
            <h2 className="is-inline has-text-weight-medium is-size-3 mr-2">
              #{pullRequest.id} <PullRequestTitle pullRequest={pullRequest} />
            </h2>
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
        <div className="media mb-4">
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
          </div>
        </div>

        <LevelWrapper className="level">
          <div className="level-left">
            <div className="level-item">
              <ApprovalContainer repository={repository} pullRequest={pullRequest} />
            </div>
          </div>
          <div className="level-right">
            <div className="level-item">{rejectButton}</div>
            <div className="level-item">{mergeButton}</div>
            <div className="level-item">{deleteSourceButton}</div>
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
        targetBranchDeleted={targetBranchDeleted}
      />
    </ChangeNotificationContext>
  );
};

export default PullRequestDetails;
