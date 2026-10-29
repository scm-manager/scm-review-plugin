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
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { PullRequest } from "../types/PullRequest";

export const DIFF_COLLAPSE_STORAGE_KEY_PREFIX = "scm-review-plugin.diff-collapse-state.v1";
export const ANONYMOUS_COLLAPSE_STATE_OWNER = "guest";
export const MAX_STORED_PULL_REQUESTS = 50;

type CollapseOverrides = Record<string, boolean>;

export type PersistedDiffCollapseState = {
  defaultCollapsed: boolean;
  overrides: CollapseOverrides;
  reviewMarks: string[];
  lastAccessed: number;
};

type PersistedCollapseStates = {
  version: 1;
  pullRequests: Record<string, PersistedDiffCollapseState>;
};

type CollapseStateActions = {
  openFileCommentEditor: (file: FileDiff) => void;
  unmarkAsReviewed: (file: FileDiff) => void;
  markAsReviewed: (file: FileDiff) => void;
  toggleCollapse: (file: FileDiff) => void;
  uncollapseAll: () => void;
  collapseAll: () => void;
};

export type DiffCollapseState = {
  isCollapsed: (file: FileDiff) => boolean;
  allCollapsed: boolean;
  actions: CollapseStateActions;
};

const emptyStorage = (): PersistedCollapseStates => ({
  version: 1,
  pullRequests: {},
});

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const normalizeEntry = (value: unknown): PersistedDiffCollapseState | undefined => {
  if (!isRecord(value) || typeof value.defaultCollapsed !== "boolean" || typeof value.lastAccessed !== "number") {
    return undefined;
  }

  const overrides: CollapseOverrides = {};
  if (isRecord(value.overrides)) {
    Object.entries(value.overrides).forEach(([path, collapsed]) => {
      if (typeof collapsed === "boolean") {
        overrides[path] = collapsed;
      }
    });
  }

  const reviewMarks = Array.isArray(value.reviewMarks)
    ? value.reviewMarks.filter((path): path is string => typeof path === "string")
    : [];

  return {
    defaultCollapsed: value.defaultCollapsed,
    overrides,
    reviewMarks,
    lastAccessed: value.lastAccessed,
  };
};

const parseStorage = (value: string | null): PersistedCollapseStates => {
  if (!value) {
    return emptyStorage();
  }

  try {
    const parsed: unknown = JSON.parse(value);
    if (!isRecord(parsed) || parsed.version !== 1 || !isRecord(parsed.pullRequests)) {
      return emptyStorage();
    }

    const pullRequests: Record<string, PersistedDiffCollapseState> = {};
    Object.entries(parsed.pullRequests).forEach(([key, entry]) => {
      const normalized = normalizeEntry(entry);
      if (normalized) {
        pullRequests[key] = normalized;
      }
    });
    return { version: 1, pullRequests };
  } catch (error) {
    console.warn("Could not read persisted diff collapse states", error);
    return emptyStorage();
  }
};

const getStorage = (): Storage | undefined => {
  try {
    return typeof window === "undefined" ? undefined : window.localStorage;
  } catch (error) {
    console.warn("Local storage is unavailable for diff collapse states", error);
    return undefined;
  }
};

export const createStorageKey = (owner: string) => `${DIFF_COLLAPSE_STORAGE_KEY_PREFIX}.${encodeURIComponent(owner)}`;

export const createPullRequestKey = (repository: Repository, pullRequest: PullRequest) =>
  JSON.stringify([repository.namespace, repository.name, pullRequest.id]);

export const createFileKey = (file: FileDiff) => (file.newPath !== "/dev/null" ? file.newPath : file.oldPath);

const createCollapsedOverrides = (paths: string[]) =>
  paths.reduce<CollapseOverrides>((overrides, path) => {
    overrides[path] = true;
    return overrides;
  }, {});

const compactOverride = (state: PersistedDiffCollapseState, path: string, collapsed: boolean) => {
  const overrides = { ...state.overrides };
  if (collapsed === state.defaultCollapsed) {
    delete overrides[path];
  } else {
    overrides[path] = collapsed;
  }
  return overrides;
};

export const createInitialState = (reviewMarks: string[], now = Date.now()): PersistedDiffCollapseState => ({
  defaultCollapsed: false,
  overrides: createCollapsedOverrides(reviewMarks),
  reviewMarks: [...reviewMarks],
  lastAccessed: now,
});

export const reconcileReviewMarks = (
  state: PersistedDiffCollapseState,
  reviewMarks: string[],
  now = Date.now(),
): PersistedDiffCollapseState => {
  const previousMarks = new Set(state.reviewMarks);
  const currentMarks = new Set(reviewMarks);
  let overrides = { ...state.overrides };

  reviewMarks.forEach((path) => {
    if (!previousMarks.has(path)) {
      overrides = compactOverride({ ...state, overrides }, path, true);
    }
  });
  state.reviewMarks.forEach((path) => {
    if (!currentMarks.has(path)) {
      overrides = compactOverride({ ...state, overrides }, path, false);
    }
  });

  return {
    ...state,
    overrides,
    reviewMarks: [...reviewMarks],
    lastAccessed: now,
  };
};

const readState = (storageKey: string | undefined, pullRequestKey: string, reviewMarks: string[], now = Date.now()) => {
  if (!storageKey) {
    return createInitialState(reviewMarks, now);
  }

  const storage = getStorage();
  try {
    const stored = storage ? parseStorage(storage.getItem(storageKey)).pullRequests[pullRequestKey] : undefined;
    return stored ? reconcileReviewMarks(stored, reviewMarks, now) : createInitialState(reviewMarks, now);
  } catch (error) {
    console.warn("Could not read persisted diff collapse states", error);
    return createInitialState(reviewMarks, now);
  }
};

const writeState = (storageKey: string | undefined, pullRequestKey: string, state: PersistedDiffCollapseState) => {
  if (!storageKey) {
    return;
  }

  const storage = getStorage();
  if (!storage) {
    return;
  }

  try {
    const stored = parseStorage(storage.getItem(storageKey));
    stored.pullRequests[pullRequestKey] = state;

    const retainedEntries = Object.entries(stored.pullRequests)
      .sort(([, left], [, right]) => right.lastAccessed - left.lastAccessed)
      .slice(0, MAX_STORED_PULL_REQUESTS);
    const value: PersistedCollapseStates = {
      version: 1,
      pullRequests: Object.fromEntries(retainedEntries),
    };

    storage.setItem(storageKey, JSON.stringify(value));
  } catch (error) {
    console.warn("Could not persist diff collapse states", error);
  }
};

export const getCollapsed = (state: PersistedDiffCollapseState, path: string) =>
  state.overrides[path] ?? state.defaultCollapsed;

const setCollapsed = (state: PersistedDiffCollapseState, path: string, collapsed: boolean) => ({
  ...state,
  overrides: compactOverride(state, path, collapsed),
  lastAccessed: Date.now(),
});

export const useDiffCollapseState = (
  repository: Repository,
  pullRequest: PullRequest,
  owner?: string,
): DiffCollapseState => {
  const pullRequestKey = useMemo(
    () => createPullRequestKey(repository, pullRequest),
    [repository.namespace, repository.name, pullRequest.id],
  );
  const storageKey = useMemo(
    () => (owner && pullRequest.id ? createStorageKey(owner) : undefined),
    [owner, pullRequest.id],
  );
  const reviewMarks = useMemo(() => pullRequest.markedAsReviewed || [], [pullRequest.markedAsReviewed]);
  const activeScope = `${storageKey || "volatile"}:${pullRequestKey}`;
  const activeScopeRef = useRef(activeScope);
  const [state, setState] = useState(() => readState(storageKey, pullRequestKey, reviewMarks));

  useEffect(() => {
    if (activeScopeRef.current !== activeScope) {
      activeScopeRef.current = activeScope;
      const restoredState = readState(storageKey, pullRequestKey, reviewMarks);
      setState(restoredState);
      writeState(storageKey, pullRequestKey, restoredState);
      return;
    }

    setState((current) => {
      const reconciled = reconcileReviewMarks(current, reviewMarks);
      writeState(storageKey, pullRequestKey, reconciled);
      return reconciled;
    });
  }, [activeScope, pullRequestKey, reviewMarks, storageKey]);

  useEffect(() => {
    if (!storageKey) {
      return undefined;
    }

    const listener = (event: StorageEvent) => {
      if (event.key !== storageKey) {
        return;
      }

      const storedState = parseStorage(event.newValue).pullRequests[pullRequestKey];
      setState(
        storedState
          ? reconcileReviewMarks(storedState, reviewMarks, storedState.lastAccessed)
          : createInitialState(reviewMarks),
      );
    };

    window.addEventListener("storage", listener);
    return () => window.removeEventListener("storage", listener);
  }, [pullRequestKey, reviewMarks, storageKey]);

  const updateState = useCallback(
    (update: (current: PersistedDiffCollapseState) => PersistedDiffCollapseState) => {
      setState((current) => {
        const updated = update(current);
        writeState(storageKey, pullRequestKey, updated);
        return updated;
      });
    },
    [pullRequestKey, storageKey],
  );

  const actions = useMemo<CollapseStateActions>(
    () => ({
      openFileCommentEditor: (file) => updateState((current) => setCollapsed(current, createFileKey(file), false)),
      unmarkAsReviewed: (file) =>
        updateState((current) => {
          const path = createFileKey(file);
          const state = { ...current, reviewMarks: current.reviewMarks.filter((reviewMark) => reviewMark !== path) };
          return setCollapsed(state, path, false);
        }),
      markAsReviewed: (file) =>
        updateState((current) => {
          const path = createFileKey(file);
          const reviewMarks = current.reviewMarks.includes(path) ? current.reviewMarks : [...current.reviewMarks, path];
          return setCollapsed({ ...current, reviewMarks }, path, true);
        }),
      toggleCollapse: (file) =>
        updateState((current) => {
          const path = createFileKey(file);
          return setCollapsed(current, path, !getCollapsed(current, path));
        }),
      uncollapseAll: () =>
        updateState((current) => ({
          ...current,
          defaultCollapsed: false,
          overrides: createCollapsedOverrides(current.reviewMarks),
          lastAccessed: Date.now(),
        })),
      collapseAll: () =>
        updateState((current) => ({ ...current, defaultCollapsed: true, overrides: {}, lastAccessed: Date.now() })),
    }),
    [updateState],
  );

  return {
    isCollapsed: (file) => getCollapsed(state, createFileKey(file)),
    allCollapsed: state.defaultCollapsed,
    actions,
  };
};
