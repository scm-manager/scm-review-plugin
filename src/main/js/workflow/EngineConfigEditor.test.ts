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

import { bySortKey } from "./EngineConfigEditor";

describe("test bySortKey", () => {
  const translations: { [key: string]: string } = {
    "workflow.rule.first.name": "first",
    "workflow.rule.first.sortKey": "aaa",
    "workflow.rule.second.name": "second",
    "workflow.rule.second.sortKey": "bbb",
    "workflow.rule.third.name": "third",
    "workflow.rule.third.sortKey": "ccc",
    "workflow.rule.last.name": "last",
    "workflow.rule.last.sortKey": "zzz",
    "workflow.rule.sortKeyOnlyFirst.sortKey": "aaa",
    "workflow.rule.sortKeyOnlyLast.sortKey": "zzz",
    "workflow.rule.nameOnlyFirst.name": "aaa",
    "workflow.rule.nameOnlyLast.name": "zzz",
    "workflow.rule.sortKeyWithFirstName.name": "aaa",
    "workflow.rule.sortKeyWithFirstName.sortKey": "sortKey",
    "workflow.rule.sortKeyWithLastName.name": "zzz",
    "workflow.rule.sortKeyWithLastName.sortKey": "sortKey"
  };

  const translateFn = (key: string) => translations[key];

  const tests = [
    ["second", "last"], // sorted by sort key, not name (bbb > zzz but s < l)
    ["first", "unkown"], // one known one invalid
    ["nameOnlyFirst", "sortKeyOnlyLast"], // one with name only other with sort key only
    ["sortKeyWithFirstName", "sortKeyWithLastName"] // Same sort key but different names
  ];

  const equalTests = [
    ["first", "first"], // both have same name and same sort key
    ["these rules", "dont exist"], // both dont exist
    ["nameOnlyFirst", "sortKeyOnlyFirst"], // name of a is same as sort key of b
    ["sortKeyOnlyLast", "nameOnlyLast"] // sort key of a is same as name of b
  ];

  it("should sort rule a before rule b", () => {
    tests.forEach(([a, b]) => expect(bySortKey(a, b, translateFn)).toBe(-1));
  });

  it("should sort both rules equally", () => {
    equalTests.forEach(([a, b]) => expect(bySortKey(a, b, translateFn)).toBe(0));
  });

  it("should sort rule b before rule a", () => {
    tests.forEach(([a, b]) => expect(bySortKey(b, a, translateFn)).toBe(1));
  });
});
