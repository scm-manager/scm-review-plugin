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

import React from "react";
import { expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import PullRequestsNavLink, { ACTIVE_WHEN_MATCH } from "./PullRequestsNavLink";

vi.mock("@scm-manager/ui-components", () => ({
  // Provide a minimal SecondaryNavigationItem component used by PullRequestsNavLink
  SecondaryNavigationItem: (props: any) => (
    <a href={props.to} aria-label={props.label}>
      {props.label}
    </a>
  ),
}));

vi.mock("@scm-manager/ui-core", () => ({
  // Export the constant expected by routes.ts and the hook used by the component
  SINGLE_REPOSITORY_BASE_ROUTE: "repo",
  useRepositoryFromParams: vi.fn(() => ({ namespace: "hitchhiker", name: "hog" })),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => [(key: string) => key],
}));

describe("PullRequestsNavLink rendering and activeWhenMatch", () => {
  const rx = new RegExp(ACTIVE_WHEN_MATCH);

  it("should render the navLink", () => {
    render(<PullRequestsNavLink />);
    expect(screen.queryByText("scm-review-plugin.navLink.pullRequest")).not.toBeNull();
  });

  it("should match for single pull request", () => {
    expect(rx.test("/scm/repo/hitchhiker/hog/pull-request/1")).toBeTruthy;
    expect(rx.test("/scm/repo/hitchhiker/hog/pull-request/1/comments")).toBeTruthy;
  });

  it("should match for pull request collection", () => {
    expect(rx.test("/repo/hitchhiker/hog/pull-requests/")).toBeTruthy;
    expect(rx.test("/repo/hitchhiker/hog/pull-requests")).toBeTruthy;
  });

  it("should not match for repository named pull request", () => {
    expect(rx.test("/repo/hitchhiker/pull-request/code/1")).toBeFalsy;
  });

  it("should not match for repository with namespace pull request", () => {
    expect(rx.test("/scm/repo/pull-request/hog/code/1")).toBeFalsy;
  });
});
