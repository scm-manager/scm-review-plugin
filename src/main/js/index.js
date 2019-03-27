// @flow
import React from "react";
import { binder } from "@scm-manager/ui-extensions";
import Create from "./Create";
import SinglePullRequest from "./SinglePullRequest";
import PullRequestList from "./PullRequestList";
import { Route } from "react-router-dom";
import PullRequestsNavLink from "./PullRequestsNavLink";

const reviewSupportedPredicate = (props: Object) => {
  return props.repository && props.repository._links.pullRequest;
};

// new

const NewPullRequestRoute = (props) => {
  return (
    <Route
      path={`${props.url}/pull-requests/add`}
      render={() => <Create repository={props.repository} userAutocompleteLink={getUserAutoCompleteLink(props.indexLinks)} />}
    />
  );
};

binder.bind("repository.route", NewPullRequestRoute);

//  show single pullRequest

function getUserAutoCompleteLink(indexLinks) {
  if (indexLinks && indexLinks.autocomplete) {
    const link = indexLinks.autocomplete.find(
      i => i.name === "users"
    );
    if (link) {
      return link.href;
    }
  }
  return "";
};

const ShowPullRequestRoute = (props ) => {
  return (
    <Route
      path={`${props.url}/pull-request/:pullRequestNumber`}
      render={() => <SinglePullRequest repository={props.repository} userAutocompleteLink={getUserAutoCompleteLink(props.indexLinks)} />}
    />
  );
};

binder.bind("repository.route", ShowPullRequestRoute);

// list

function matches(route: any) {
  const regex = new RegExp(".*(/pull-request)/.*");
  return (
    route.location.pathname.match(regex) ||
    route.location.pathname.match(".*(pull-requests)/.*")
  );
}

const PullRequestNavLink = ({ url }) => {
  return <PullRequestsNavLink url={url} activeWhenMatch={matches} />;
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
