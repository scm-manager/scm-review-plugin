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

import { isRouteMatching } from "./isRouteMatching";

describe("route match", () => {
  it("should match for single pull request", () => {
    expect(isRouteMatching({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-request/1" } })).toBeTruthy();
    expect(
      isRouteMatching({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-request/1/comments" } })
    ).toBeTruthy();
  });
  it("should match for collection", () => {
    expect(isRouteMatching({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-requests/" } })).toBeTruthy();
    expect(isRouteMatching({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-requests" } })).toBeTruthy();
  });
  it("should not match for repository named pull-request", () => {
    expect(isRouteMatching({ location: { pathname: "/scm/repo/hitchhiker/pull-request/code/1" } })).toBeFalsy();
  });
  it("should not match for repository with namespace pull-request", () => {
    expect(isRouteMatching({ location: { pathname: "/scm/repo/pull-request/hog/code/1" } })).toBeFalsy();
  });
});