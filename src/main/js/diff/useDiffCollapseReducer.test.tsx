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

import { FileDiff, Repository } from "@scm-manager/ui-types";
import { act, renderHook as testingLibraryRenderHook } from "@testing-library/react";
import { vi, type MockInstance } from "vitest";
import { PullRequest } from "../types/PullRequest";
import {
  ANONYMOUS_COLLAPSE_STATE_OWNER,
  createFileKey,
  createPullRequestKey,
  createStorageKey,
  DiffCollapseState,
  MAX_STORED_PULL_REQUESTS,
  useDiffCollapseState,
} from "./useDiffCollapseReducer";

const repository = {
  namespace: "hitchhikers",
  name: "guide",
} as Repository;

const createPullRequest = (id: string, reviewMarks: string[] = []): PullRequest =>
  ({
    id,
    source: "feature",
    target: "main",
    markedAsReviewed: reviewMarks,
  }) as PullRequest;

const createFile = (path: string, oldPath = path): FileDiff => ({
  newPath: path,
  oldPath,
  newEndingNewLine: true,
  oldEndingNewLine: true,
  type: path === "/dev/null" ? "delete" : "modify",
});

type HookProps = {
  repository: Repository;
  pullRequest: PullRequest;
  owner?: string;
};

const renderHook = (pullRequest = createPullRequest("1"), owner = "trillian", repositoryValue = repository) => {
  let props: HookProps = {
    repository: repositoryValue,
    pullRequest,
    owner,
  };
  const hook = testingLibraryRenderHook(
    ({ repository: repositoryProp, pullRequest: pullRequestProp, owner: ownerProp }) =>
      useDiffCollapseState(repositoryProp, pullRequestProp, ownerProp),
    { initialProps: props },
  );

  return {
    get current() {
      return hook.result.current;
    },
    wrapper: {
      setProps: (nextProps: Partial<HookProps>) => {
        props = { ...props, ...nextProps };
        hook.rerender(props);
      },
      update: () => undefined,
      unmount: hook.unmount,
    },
  };
};

const update = (hook: ReturnType<typeof renderHook>, action: () => void) => {
  act(action);
  hook.wrapper.update();
};

describe("useDiffCollapseState", () => {
  let warn: MockInstance;

  beforeEach(() => {
    localStorage.clear();
    warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
  });

  afterEach(() => {
    warn.mockRestore();
    vi.restoreAllMocks();
  });

  it("restores manually collapsed files for the same account, repository and pull request", () => {
    const file = createFile("README.md");
    const firstVisit = renderHook();

    update(firstVisit, () => firstVisit.current.actions.toggleCollapse(file));
    expect(firstVisit.current.isCollapsed(file)).toBe(true);
    firstVisit.wrapper.unmount();

    const nextVisit = renderHook({
      ...createPullRequest("1"),
      sourceRevision: "a-new-revision",
      targetRevision: "another-revision",
    });
    expect(nextVisit.current.isCollapsed(file)).toBe(true);
  });

  it("isolates stored state by account, repository and pull request", () => {
    const file = createFile("README.md");
    const original = renderHook();
    update(original, () => original.current.actions.toggleCollapse(file));
    original.wrapper.unmount();

    expect(renderHook(createPullRequest("1"), "arthur").current.isCollapsed(file)).toBe(false);
    expect(renderHook(createPullRequest("2")).current.isCollapsed(file)).toBe(false);
    expect(
      renderHook(createPullRequest("1"), "trillian", { ...repository, name: "restaurant" }).current.isCollapsed(file),
    ).toBe(false);
  });

  it("persists collapse state for the browser-local guest", () => {
    const file = createFile("public.md");
    const firstVisit = renderHook(createPullRequest("1"), ANONYMOUS_COLLAPSE_STATE_OWNER);
    update(firstVisit, () => firstVisit.current.actions.toggleCollapse(file));
    firstVisit.wrapper.unmount();

    expect(renderHook(createPullRequest("1"), ANONYMOUS_COLLAPSE_STATE_OWNER).current.isCollapsed(file)).toBe(true);
  });

  it("uses review marks initially and reconciles later server changes without losing explicit state", () => {
    const reviewed = createFile("reviewed.ts");
    const newlyReviewed = createFile("newly-reviewed.ts");
    const hook = renderHook(createPullRequest("1", [reviewed.newPath]));

    expect(hook.current.isCollapsed(reviewed)).toBe(true);
    update(hook, () => hook.current.actions.toggleCollapse(reviewed));
    expect(hook.current.isCollapsed(reviewed)).toBe(false);

    act(() => {
      hook.wrapper.setProps({
        pullRequest: createPullRequest("1", [reviewed.newPath, newlyReviewed.newPath]),
      });
    });
    hook.wrapper.update();
    expect(hook.current.isCollapsed(reviewed)).toBe(false);
    expect(hook.current.isCollapsed(newlyReviewed)).toBe(true);

    act(() => {
      hook.wrapper.setProps({ pullRequest: createPullRequest("1", [newlyReviewed.newPath]) });
    });
    hook.wrapper.update();
    expect(hook.current.isCollapsed(reviewed)).toBe(false);
  });

  it("keeps the current effects of mark, unmark and opening a file comment", () => {
    const file = createFile("answer.ts");
    const hook = renderHook();

    update(hook, () => hook.current.actions.markAsReviewed(file));
    expect(hook.current.isCollapsed(file)).toBe(true);
    update(hook, () => hook.current.actions.unmarkAsReviewed(file));
    expect(hook.current.isCollapsed(file)).toBe(false);
    update(hook, () => hook.current.actions.markAsReviewed(file));
    update(hook, () => hook.current.actions.openFileCommentEditor(file));
    expect(hook.current.isCollapsed(file)).toBe(false);
  });

  it("keeps optimistic review marks in sync for an immediate bulk state change", () => {
    const newlyReviewed = createFile("newly-reviewed.ts");
    const newlyUnreviewed = createFile("newly-unreviewed.ts");
    const hook = renderHook(createPullRequest("1", [newlyUnreviewed.newPath]));

    update(hook, () => hook.current.actions.markAsReviewed(newlyReviewed));
    update(hook, () => hook.current.actions.unmarkAsReviewed(newlyUnreviewed));
    update(hook, hook.current.actions.collapseAll);
    update(hook, hook.current.actions.uncollapseAll);

    expect(hook.current.isCollapsed(newlyReviewed)).toBe(true);
    expect(hook.current.isCollapsed(newlyUnreviewed)).toBe(false);
  });

  it("expands only unreviewed files when reverting the bulk collapse state and persists the result", () => {
    const reviewed = createFile("reviewed.ts");
    const loaded = createFile("loaded.ts");
    const loadedLater = createFile("loaded-later.ts");
    const pullRequest = createPullRequest("1", [reviewed.newPath]);
    const hook = renderHook(pullRequest);

    update(hook, hook.current.actions.collapseAll);
    expect(hook.current.allCollapsed).toBe(true);
    expect(hook.current.isCollapsed(reviewed)).toBe(true);
    expect(hook.current.isCollapsed(loaded)).toBe(true);
    expect(hook.current.isCollapsed(loadedLater)).toBe(true);

    update(hook, () => hook.current.actions.toggleCollapse(loaded));
    expect(hook.current.isCollapsed(loaded)).toBe(false);
    expect(hook.current.isCollapsed(loadedLater)).toBe(true);

    update(hook, hook.current.actions.uncollapseAll);
    expect(hook.current.allCollapsed).toBe(false);
    expect(hook.current.isCollapsed(reviewed)).toBe(true);
    expect(hook.current.isCollapsed(loaded)).toBe(false);
    expect(hook.current.isCollapsed(loadedLater)).toBe(false);

    hook.wrapper.unmount();
    const restored = renderHook(pullRequest);
    expect(restored.current.allCollapsed).toBe(false);
    expect(restored.current.isCollapsed(reviewed)).toBe(true);
    expect(restored.current.isCollapsed(loaded)).toBe(false);
  });

  it("persists a manual expansion of a reviewed file", () => {
    const reviewed = createFile("reviewed.ts");
    const pullRequest = createPullRequest("1", [reviewed.newPath]);
    const hook = renderHook(pullRequest);

    update(hook, () => hook.current.actions.toggleCollapse(reviewed));
    expect(hook.current.isCollapsed(reviewed)).toBe(false);
    hook.wrapper.unmount();

    expect(renderHook(pullRequest).current.isCollapsed(reviewed)).toBe(false);
  });

  it("uses the old path as the identity of a deleted file", () => {
    expect(createFileKey(createFile("/dev/null", "obsolete.ts"))).toBe("obsolete.ts");
  });

  it("updates an open view when another tab changes its storage entry", () => {
    const file = createFile("shared.ts");
    const pullRequest = createPullRequest("1");
    const owner = "ford";
    const hook = renderHook(pullRequest, owner);
    const storageKey = createStorageKey(owner);
    const pullRequestKey = createPullRequestKey(repository, pullRequest);
    const newValue = JSON.stringify({
      version: 1,
      pullRequests: {
        [pullRequestKey]: {
          defaultCollapsed: false,
          overrides: { [file.newPath]: true },
          reviewMarks: [],
          lastAccessed: 42,
        },
      },
    });
    localStorage.setItem(storageKey, newValue);

    act(() => {
      window.dispatchEvent(new StorageEvent("storage", { key: storageKey, newValue }));
    });
    hook.wrapper.update();

    expect(hook.current.isCollapsed(file)).toBe(true);
  });

  it("retains only the most recently used pull requests", () => {
    let now = 0;
    vi.spyOn(Date, "now").mockImplementation(() => ++now);

    for (let id = 0; id <= MAX_STORED_PULL_REQUESTS; id += 1) {
      const hook = renderHook(createPullRequest(String(id)), "zaphod");
      hook.wrapper.unmount();
    }

    const stored = JSON.parse(localStorage.getItem(createStorageKey("zaphod")) || "{}");
    expect(Object.keys(stored.pullRequests)).toHaveLength(MAX_STORED_PULL_REQUESTS);
    expect(stored.pullRequests[createPullRequestKey(repository, createPullRequest("0"))]).toBeUndefined();
    expect(stored.pullRequests[createPullRequestKey(repository, createPullRequest("50"))]).toBeDefined();
  });

  it("falls back to usable in-memory state for malformed or failing storage", () => {
    const storageKey = createStorageKey("marvin");
    localStorage.setItem(storageKey, "not-json");
    const malformed = renderHook(createPullRequest("1"), "marvin");
    expect(malformed.current.isCollapsed(createFile("safe.ts"))).toBe(false);
    expect(warn).toHaveBeenCalled();
    malformed.wrapper.unmount();

    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota exceeded");
    });
    const failing = renderHook(createPullRequest("2"), "marvin");
    const file = createFile("still-usable.ts");
    update(failing, () => failing.current.actions.toggleCollapse(file));
    expect(failing.current.isCollapsed(file)).toBe(true);
  });

  it("does not persist before a pull request id is available", () => {
    const file = createFile("volatile.ts");
    const withoutId = renderHook(createPullRequest("") as PullRequest, "slartibartfast");
    update(withoutId, () => withoutId.current.actions.toggleCollapse(file));
    expect(localStorage.length).toBe(0);
  });
});
