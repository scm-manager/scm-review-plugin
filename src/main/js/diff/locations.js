// @flow

import type {BaseContext, DiffEventContext} from "@scm-manager/ui-components";
import {diffs} from "@scm-manager/ui-components";
import type {Location} from "../types/PullRequest";

export function createHunkId(context: BaseContext): string {
  return diffs.getPath(context.file) + "_" + context.hunk.content;
}

export function createHunkIdFromLocation(location: Location): string {
  if (!location.hunk) {
    throw new Error("only locations with a hunk could be used");
  }
  return location.file + "_" + location.hunk;
}

export function isInlineLocation(location: Location): boolean {
  return !!location.hunk;
}

export function createFileLocation(context: BaseContext): Location {
  return {
    file: diffs.getPath(context.file)
  }
}

export function createInlineLocation(
  context: DiffEventContext
): Location {
  const change = context.change;

  const location: Location = {
    file: diffs.getPath(context.file),
    hunk: context.hunk.content
  };

  switch (change.type) {
    case "insert":
      location.newLineNumber = change.lineNumber;
      break;
    case "delete":
      location.oldLineNumber = change.lineNumber;
      break;
    case "normal":
      location.newLineNumber = change.newLineNumber;
      location.oldLineNumber = change.oldLineNumber;
      break;
    default:
      throw new Error(`illegal change type ${change.type}`);
  }

  return location;
}

export function createChangeIdFromLocation(location: Location) {
  if (location.oldLineNumber && location.newLineNumber) {
    return "N" + location.oldLineNumber;
  } else if (location.newLineNumber) {
    return "I" + location.newLineNumber;
  } else if (location.oldLineNumber) {
    return "D" + location.oldLineNumber;
  } else {
    throw new Error("at least one line number has to be set");
  }
}
