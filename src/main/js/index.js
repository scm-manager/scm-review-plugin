// @flow
import React from "react";
import { binder } from "@scm-manager/ui-extensions";
import { NavLink } from "@scm-manager/ui-components";
import Create from "./Create";
import SinglePullRequest from "./SinglePullRequest";
import PullRequestList from "./PullRequestList";
import { Route } from "react-router-dom";

const reviewSupportedPredicate = (props: Object) => {
  return props.repository && props.repository._links.pullRequest;
};

// new

const NewPullRequestRoute = ({ url, repository }) => {
  return (
    <Route
      path={`${url}/pull-requests/add`}
      render={() => <Create repository={repository} />}
      exact
    />
  );
};

binder.bind("repository.route", NewPullRequestRoute);

//  show single pullRequest

const ShowPullRequestRoute = ({ url, repository }) => {
  return (
    <Route
      path={`${url}/pull-request/:pullRequestNumber`}
      render={() => <SinglePullRequest repository={repository} />}
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
  return (
    <NavLink
      to={`${url}/pull-requests`}
      label="Pull Requests"
      activeWhenMatch={matches}
    />
  );
};

binder.bind(
  "repository.navigation",
  PullRequestNavLink,
  reviewSupportedPredicate
);

const ShowPullRequestsRoute = ({ url, repository }) => {
  return (
    <Route
      path={`${url}/pull-requests/`}
      render={() => <PullRequestList repository={repository} />}
      exact
    />
  );
};

binder.bind("repository.route", ShowPullRequestsRoute);
