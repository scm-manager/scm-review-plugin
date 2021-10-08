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
import { matches } from "./index";

describe("route match", () => {
  it("should match for single pull request", () => {
    expect(matches({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-request/1" } })).toBeTruthy();
    expect(matches({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-request/1/comments" } })).toBeTruthy();
  });
  it("should match for collection", () => {
    expect(matches({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-requests/" } })).toBeTruthy();
    expect(matches({ location: { pathname: "/scm/repo/hitchhiker/hog/pull-requests" } })).toBeTruthy();
  });
  it("should not match for repository named pull-request", () => {
    expect(matches({ location: { pathname: "/scm/repo/hitchhiker/pull-request/code/1" } })).toBeFalsy();
  });
  it("should not match for repository with namespace pull-request", () => {
    expect(matches({ location: { pathname: "/scm/repo/pull-request/hog/code/1" } })).toBeFalsy();
  });
});
