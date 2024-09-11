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

import { isUrlSuffixMatching } from "./PullRequestInformation";

describe("test isUrlSuffixMatching", () => {
  it("should return true", () => {
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix", "suffix")).toBe(true);
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1", "suffix")).toBe(true);
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1/2", "suffix")).toBe(true);
  });

  it("should return false", () => {
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix", "other")).toBe(false);
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1", "other")).toBe(false);
    expect(isUrlSuffixMatching("/my/base", "my/base/suffix/1/2", "other")).toBe(false);
  });
});
