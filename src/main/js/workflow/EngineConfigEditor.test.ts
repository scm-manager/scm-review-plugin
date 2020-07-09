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
import { bySortKey } from "./EngineConfigEditor";

describe("test bySortKey", () => {
  const translations: {[key: string]: string} = {
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
    "workflow.rule.sortKeyWithLastName.sortKey": "sortKey",
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
  ]

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
