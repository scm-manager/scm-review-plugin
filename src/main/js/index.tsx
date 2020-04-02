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
import { ConfigurationBinder as cfgBinder } from "@scm-manager/ui-components";
import { binder } from "@scm-manager/ui-extensions";
import Create from "./Create";
import SinglePullRequest from "./SinglePullRequest";
import PullRequestList from "./PullRequestList";
import { Route } from "react-router-dom";
import PullRequestsNavLink from "./PullRequestsNavLink";
import CreatePullRequestButton from "./CreatePullRequestButton";
import RepositoryConfig from "./config/RepositoryConfig";
import GlobalConfig from "./config/GlobalConfig";
import RepositoryPullRequestCardLink from "./RepositoryPullRequestCardLink";
import MyPullRequest from "./landingpage/MyPullRequest";
import PullRequestCreatedEvent from "./landingpage/PullRequestCreatedEvent";

const reviewSupportedPredicate = (props: object) => {
  return props.repository && props.repository._links.pullRequest;
};

// new

const NewPullRequestRoute = props => {
  return (
    <Route
      path={`${props.url}/pull-requests/add`}
      render={() => (
        <Create repository={props.repository} userAutocompleteLink={getUserAutoCompleteLink(props.indexLinks)} />
      )}
    />
  );
};

binder.bind("repository.route", NewPullRequestRoute);

//  show single pullRequest

export function getUserAutoCompleteLink(indexLinks) {
  if (indexLinks && indexLinks.autocomplete) {
    const link = indexLinks.autocomplete.find(i => i.name === "users");
    if (link) {
      return link.href;
    }
  }
  return "";
}

const ShowPullRequestRoute = props => {
  return (
    <Route
      path={`${props.url}/pull-request/:pullRequestNumber`}
      render={() => (
        <SinglePullRequest
          repository={props.repository}
          userAutocompleteLink={getUserAutoCompleteLink(props.indexLinks)}
        />
      )}
    />
  );
};

binder.bind("repository.route", ShowPullRequestRoute);

// list

function matches(route: any) {
  const regex = new RegExp(".*(/pull-request)/.*");
  return route.location.pathname.match(regex) || route.location.pathname.match(".*(pull-requests)/.*");
}

const PullRequestNavLink = ({ url }) => {
  return <PullRequestsNavLink url={url} activeWhenMatch={matches} />;
};

binder.bind("repository.navigation", PullRequestNavLink, reviewSupportedPredicate);

const ShowPullRequestsRoute = ({ url, repository }) => {
  return <Route path={`${url}/pull-requests/`} render={() => <PullRequestList repository={repository} />} exact />;
};

binder.bind("repository.route", ShowPullRequestsRoute);

binder.bind("repos.branch-details.information", ({ repository, branch }) => (
  <CreatePullRequestButton repository={repository} branch={branch} />
));

binder.bind("repository.card.quickLink", RepositoryPullRequestCardLink, reviewSupportedPredicate);

cfgBinder.bindRepositorySetting("/review", "scm-review-plugin.navLink", "pullRequestConfig", RepositoryConfig);
cfgBinder.bindGlobal("/review", "scm-review-plugin.navLink", "pullRequestConfig", GlobalConfig);

binder.bind("landingpage.mydata", {
  render: (data: any, key: any) => <MyPullRequest key={key} data={data} />,
  title: "scm-review-plugin.landingpage.myPullRequests.title",
  separatedEntries: true,
  type: "MyPullRequestData"
});
binder.bind("landingpage.myevents", PullRequestCreatedEvent);
