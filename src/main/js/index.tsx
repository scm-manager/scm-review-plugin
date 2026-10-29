/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC } from "react";
import { CardColumnSmall, ConfigurationBinder as cfgBinder, useShortcut } from "@scm-manager/ui-components";
import { binder, extensionPoints } from "@scm-manager/ui-extensions";
import { useNavigate } from "react-router";
import PullRequestsNavLink from "./PullRequestsNavLink";
import { Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import {
  CREATE_PULL_REQUEST_BASE,
  PULL_REQUEST_COLLECTION_BASE,
  PULL_REQUEST_EDIT_BASE,
  PULL_REQUEST_ID,
  resolvePullRequestOverview,
  SINGLE_PULL_REQUEST_BASE,
} from "./routes";
import PullRequestsOverview from "./PullRequestsOverview";
import Create from "./Create";
import RepositoryConfig from "./config/RepositoryConfig";
import GlobalConfig from "./config/GlobalConfig";
import RepoEngineConfig from "./workflow/RepoEngineConfig";
import GlobalEngineConfig from "./workflow/GlobalEngineConfig";
import NamespaceConfig from "./config/NamespaceConfig";
import PullRequestBannerList from "./PullRequestBannerList";
import BranchDetailsMenu from "./BranchDetailsMenu";
import BranchDetailsPullRequests from "./BranchDetailsPullRequests";
import CreatePullRequestButton from "./CreatePullRequestButton";
import CommentHitRenderer from "./search/CommentHitRenderer";
import PullRequestHitRenderer from "./search/PullRequestHitRenderer";
import ApprovedByXReviewersRuleConfiguration from "./workflow/ApprovedByXReviewersRuleConfiguration";
import PullRequestCreatedEvent from "./landingpage/PullRequestCreatedEvent";
import PullRequestDraftToOpenEvent from "./landingpage/PullRequestDraftToOpenEvent";
import PullRequestReopenedEvent from "./landingpage/PullRequestReopenedEvent";
import { DataType } from "./landingpage/DataType";
// @ts-ignore
import { MyDataExtension, MyEventExtension, MyTaskExtension } from "@scm-manager/scm-landingpage-plugin";
import PullRequestTodos from "./landingpage/MyPullRequestTodos";
import PullRequestReview from "./landingpage/MyPullRequestReview";
import { Icon } from "@scm-manager/ui-core";
import MyPullRequest from "./landingpage/MyPullRequest";
import PullRequestDetailsWrapper from "./PullRequestDetailsWrapper";
import EditWrapper from "./EditWrapper";

const reviewSupportedPredicate = (props: { repository: Repository }) => {
  return props.repository && !!props.repository._links.pullRequest;
};
//eslint-disable-next-line react/prop-types
const PullRequestNavLink: extensionPoints.RepositoryNavigation["type"] = ({ repository }) => {
  const [t] = useTranslation("plugins");
  const navigate = useNavigate();
  const url = resolvePullRequestOverview(repository);
  useShortcut("g p", () => navigate(url), {
    // eslint-disable-next-line react/prop-types
    active: !!repository?._links["pullRequest"],
    description: t("scm-review-plugin.shortcuts.pullRequests"),
  });
  return <PullRequestsNavLink />;
};
binder.bind<extensionPoints.RepositoryNavigation>(
  "repository.navigation",
  PullRequestNavLink,
  reviewSupportedPredicate,
);

const PullRequestCollectionRepositoryRoutes = () => ({
  path: PULL_REQUEST_COLLECTION_BASE,
  children: [
    {
      index: true,
      path: `:page?`,
      element: <PullRequestsOverview />,
    },
    {
      path: `${CREATE_PULL_REQUEST_BASE}/*`,
      element: <Create />,
    },
  ],
});

binder.bind("repository.route", PullRequestCollectionRepositoryRoutes);

const SinglePullRequestRepositoryRoute = () => ({
  path: `${SINGLE_PULL_REQUEST_BASE}/${PULL_REQUEST_ID}`,
  children: [
    {
      path: PULL_REQUEST_EDIT_BASE,
      element: <EditWrapper />,
    },
    {
      path: "*",
      element: <PullRequestDetailsWrapper />,
    },
  ],
});

binder.bind("repository.route", SinglePullRequestRepositoryRoute);

const AllPullRequestsLink: FC = () => {
  const [t] = useTranslation("plugins");
  return (
    <CardColumnSmall
      link={"/search/pullRequest/?q=status:OPEN OR status:DRAFT"}
      contentLeft={t("scm-review-plugin.landingpage.myPullRequests.allPullRequestsLinkTitle")}
      contentRight=""
      avatar={<Icon name="noop" className="fa-fw fa-lg" />}
    />
  );
};

binder.bind<MyDataExtension<DataType>>("landingpage.mydata", {
  render: (data: DataType) => <MyPullRequest key={data.pullRequest.id} data={data} />,
  beforeData: <AllPullRequestsLink />,
  title: "scm-review-plugin.landingpage.myPullRequests.title",
  separatedEntries: false,
  type: "MyPullRequestData",
  emptyMessage: "scm-review-plugin.landingpage.myPullRequests.emptyMessage",
});

binder.bind<extensionPoints.BranchListDetail>(
  "branches.list.detail",
  BranchDetailsPullRequests,
  ({ branchDetails, branch }) => branchDetails && !branch.defaultBranch,
);

binder.bind<extensionPoints.BranchListMenu>(
  "branches.list.menu",
  BranchDetailsMenu,
  ({ branch }) => !branch.defaultBranch,
);

binder.bind("repository.banner", PullRequestBannerList);

binder.bind<extensionPoints.ReposBranchDetailsInformation>(
  "repos.branch-details.information",
  ({ repository, branch }) => <CreatePullRequestButton repository={repository} branch={branch} />,
);

cfgBinder.bindRepositorySetting(
  "/review",
  "scm-review-plugin.navLink.pullRequest",
  "pullRequestConfig",
  RepositoryConfig,
);

cfgBinder.bindNamespaceSetting(
  "/review",
  "scm-review-plugin.navLink.pullRequest",
  "pullRequestConfig",
  NamespaceConfig,
);

cfgBinder.bindAdminSetting("/review", "scm-review-plugin.navLink.pullRequest", "pullRequestConfig", GlobalConfig);

cfgBinder.bindRepositorySetting("/workflow", "scm-review-plugin.navLink.workflow", "workflowConfig", RepoEngineConfig);
cfgBinder.bindAdminSetting("/workflow", "scm-review-plugin.navLink.workflow", "workflowConfig", GlobalEngineConfig);

binder.bind("reviewPlugin.workflow.config.ApprovedByXReviewersRule", ApprovedByXReviewersRuleConfiguration);
binder.bind("search.hit.pullRequest.renderer", PullRequestHitRenderer);
binder.bind("search.hit.indexedComment.renderer", CommentHitRenderer);

binder.bind<MyEventExtension>("landingpage.myevents", PullRequestCreatedEvent);
binder.bind<MyEventExtension>("landingpage.myevents", PullRequestDraftToOpenEvent);
binder.bind<MyEventExtension>("landingpage.myevents", PullRequestReopenedEvent);
binder.bind<MyTaskExtension<DataType>>("landingpage.mytask", PullRequestTodos);
binder.bind<MyTaskExtension<DataType>>("landingpage.mytask", PullRequestReview);
