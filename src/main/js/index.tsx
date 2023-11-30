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
import React, { FC } from "react";
import { CardColumnSmall, ConfigurationBinder as cfgBinder, Icon, useShortcut } from "@scm-manager/ui-components";
import { binder, extensionPoints } from "@scm-manager/ui-extensions";
import Create from "./Create";
import SinglePullRequest from "./SinglePullRequest";
import PullRequestsOverview from "./PullRequestsOverview";
import { Route, useHistory } from "react-router-dom";
import PullRequestsNavLink from "./PullRequestsNavLink";
import CreatePullRequestButton from "./CreatePullRequestButton";
import RepositoryConfig from "./config/RepositoryConfig";
import GlobalConfig from "./config/GlobalConfig";
import MyPullRequest from "./landingpage/MyPullRequest";
import PullRequestCreatedEvent from "./landingpage/PullRequestCreatedEvent";
import PullRequestDraftToOpenEvent from "./landingpage/PullRequestDraftToOpenEvent";
import PullRequestTodos from "./landingpage/MyPullRequestTodos";
import PullRequestReview from "./landingpage/MyPullRequestReview";
import RepoEngineConfig from "./workflow/RepoEngineConfig";
import GlobalEngineConfig from "./workflow/GlobalEngineConfig";
import ApprovedByXReviewersRuleConfiguration from "./workflow/ApprovedByXReviewersRuleConfiguration";
import { Repository } from "@scm-manager/ui-types";
import PullRequestHitRenderer from "./search/PullRequestHitRenderer";
import CommentHitRenderer from "./search/CommentHitRenderer";
import BranchDetailsMenu from "./BranchDetailsMenu";
import { useTranslation } from "react-i18next";
import NamespaceConfig from "./config/NamespaceConfig";
import { DataType } from "./landingpage/DataType";
import { MyDataExtension, MyEventExtension, MyTaskExtension } from "@scm-manager/scm-landingpage-plugin";
import BranchDetailsPullRequests from "./BranchDetailsPullRequests";
import PullRequestReopenedEvent from "./landingpage/PullRequestReopenedEvent";

type PredicateProps = {
  repository: Repository;
};

const reviewSupportedPredicate = (props: PredicateProps) => {
  return props.repository && !!props.repository._links.pullRequest;
};

// new

type RepoRouteProps = { repository: Repository; url: string };

const NewPullRequestRoute = ({ url, repository }: RepoRouteProps) => {
  return (
    <Route path={`${url}/pull-requests/add`}>
      <Create repository={repository} />
    </Route>
  );
};

binder.bind("repository.route", NewPullRequestRoute, { priority: 10000 });

const ShowPullRequestRoute = ({ url, repository }: RepoRouteProps) => {
  return (
    <Route path={`${url}/pull-request/:pullRequestNumber`}>
      <SinglePullRequest repository={repository} />
    </Route>
  );
};

binder.bind("repository.route", ShowPullRequestRoute);

const routeRegex = /.*\/repo\/.+\/.+\/pull-requests?(\/.*)?/;

export function matches(route: any) {
  return route.location.pathname.match(routeRegex);
}

const PullRequestNavLink: extensionPoints.RepositoryNavigation["type"] = ({ url, repository }) => {
  const [t] = useTranslation("plugins");
  const history = useHistory();
  useShortcut("g p", () => history.push(`${url}/pull-requests/`), {
    active: !!repository?._links["pullRequest"],
    description: t("scm-review-plugin.shortcuts.pullRequests")
  });
  return <PullRequestsNavLink url={url} activeWhenMatch={matches} />;
};

binder.bind<extensionPoints.RepositoryNavigation>(
  "repository.navigation",
  PullRequestNavLink,
  reviewSupportedPredicate
);

const ShowPullRequestsRoute = ({ url, repository }: RepoRouteProps) => {
  return (
    <>
      <Route path={`${url}/pull-requests/`} exact>
        <PullRequestsOverview repository={repository} />
      </Route>
      <Route path={`${url}/pull-requests/:page`} exact>
        <PullRequestsOverview repository={repository} />
      </Route>
    </>
  );
};

const AllPullRequestsLink: FC = () => {
  const [t] = useTranslation("plugins");
  return (
    <CardColumnSmall
      link="/search/pullRequest/?q=status:IN_PROGRESS"
      contentLeft={t("scm-review-plugin.landingpage.myPullRequests.allPullRequestsLinkTitle")}
      contentRight=""
      avatar={<Icon name="noop" className="fa-fw fa-lg" />}
    />
  );
};

binder.bind<extensionPoints.BranchListDetail>(
  "branches.list.detail",
  BranchDetailsPullRequests,
  ({ branchDetails, branch }) => branchDetails && !branch.defaultBranch
);

binder.bind<extensionPoints.BranchListMenu>(
  "branches.list.menu",
  BranchDetailsMenu,
  ({ branch }) => !branch.defaultBranch
);

binder.bind("repository.route", ShowPullRequestsRoute);

binder.bind<extensionPoints.ReposBranchDetailsInformation>(
  "repos.branch-details.information",
  ({ repository, branch }) => <CreatePullRequestButton repository={repository} branch={branch} />
);

cfgBinder.bindRepositorySetting(
  "/review",
  "scm-review-plugin.navLink.pullRequest",
  "pullRequestConfig",
  RepositoryConfig
);
cfgBinder.bindNamespaceSetting(
  "/review",
  "scm-review-plugin.navLink.pullRequest",
  "pullRequestConfig",
  NamespaceConfig
);
cfgBinder.bindGlobal("/review", "scm-review-plugin.navLink.pullRequest", "pullRequestConfig", GlobalConfig);

cfgBinder.bindRepositorySetting("/workflow", "scm-review-plugin.navLink.workflow", "workflowConfig", RepoEngineConfig);
cfgBinder.bindGlobal("/workflow", "scm-review-plugin.navLink.workflow", "workflowConfig", GlobalEngineConfig);

binder.bind<MyDataExtension<DataType>>("landingpage.mydata", {
  render: data => <MyPullRequest key={data.pullRequest.id} data={data} />,
  beforeData: <AllPullRequestsLink />,
  title: "scm-review-plugin.landingpage.myPullRequests.title",
  separatedEntries: false,
  type: "MyPullRequestData",
  emptyMessage: "scm-review-plugin.landingpage.myPullRequests.emptyMessage"
});
binder.bind<MyEventExtension>("landingpage.myevents", PullRequestCreatedEvent);
binder.bind<MyEventExtension>("landingpage.myevents", PullRequestDraftToOpenEvent);
binder.bind<MyEventExtension>("landingpage.myevents", PullRequestReopenedEvent);
binder.bind<MyTaskExtension<DataType>>("landingpage.mytask", PullRequestTodos);
binder.bind<MyTaskExtension<DataType>>("landingpage.mytask", PullRequestReview);

binder.bind("reviewPlugin.workflow.config.ApprovedByXReviewersRule", ApprovedByXReviewersRuleConfiguration);
binder.bind("search.hit.pullRequest.renderer", PullRequestHitRenderer);
binder.bind("search.hit.indexedComment.renderer", CommentHitRenderer);

export { PullRequestListDetailExtension } from "./types/ExtensionPoints";
