// @flow
import React from "react";
import { binder } from "@scm-manager/ui-extensions";
import { NavLink } from "@scm-manager/ui-components";
import Create from './Create';
import { Route } from "react-router-dom";

const reviewSupportedPredicate = (props: Object) => {
  return props.repository && props.repository._links.newPullRequest;
};

// new

const NewPullRequestNavLink = ({url}) => {
  return <NavLink to={`${url}/pull-requests/add`} label="New Pull Request"/>;
};

binder.bind("repository.navigation", NewPullRequestNavLink, reviewSupportedPredicate);

const NewPullRequestRoute = ({url, repository}) => {
  return <Route path={`${url}/pull-requests/add`}
                render={() => <Create repository={repository}/>}
                exact/>;
};

binder.bind("repository.route", NewPullRequestRoute);

// list

const PullRequestNavLink = ({url}) => {
  return <NavLink to={`${url}/pull-requests`} label="Pull Requests"/>;
};

binder.bind("repository.navigation", PullRequestNavLink, reviewSupportedPredicate);
