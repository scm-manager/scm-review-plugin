// @flow

import type {BaseContext} from '@scm-manager/ui-components';
import {getPath} from '@scm-manager/ui-components/src/repos/diffs';
import type {Location} from '../types/PullRequest';

export function createHunkId(context: BaseContext): string {
  return getPath(context.file) + "_" + context.hunk.content;
}

export function createHunkIdFromLocation(location: Location): string {
  if (!location.hunk) {
    throw new Error("only locations with a hunk could be used");
  }
  return location.file + "_" + location.hunk;
}

export function createLocation(context: BaseContext, changeId: string): Location {
  return {
    file: getPath(context.file),
    hunk: context.hunk.content,
    changeId
  };
}
