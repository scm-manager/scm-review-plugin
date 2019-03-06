// @flow
import { getPullRequestComments } from "../pullRequest";
import { createHunkIdFromLocation } from "./locations";
import type { Comment } from "../types/PullRequest";

type FileComments = {
  [string]: {
    comments: Comment[]
  }
};
type LineComments = { [string]: { [string]: Comment[] } };

function addFileComments(fileComments: FileComments, comment: Comment) {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const file = location.file;
  const fileState = fileComments[file] || {
    comments: []
  };
  fileState.comments.push(comment);

  fileComments[file] = fileState;
}

function addLineComments(lineComments: LineComments, comment: Comment) {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const changeId = location.changeId;
  if (!changeId) {
    // should never happen, mostly to make flow happy
    throw new Error("location has no change id");
  }

  const hunkId = createHunkIdFromLocation(location);
  const hunkComments = lineComments[hunkId] || {};

  const changeComments = hunkComments[changeId] || [];
  changeComments.push(comment);
  hunkComments[changeId] = changeComments;

  lineComments[hunkId] = hunkComments;
}

export function fetchComments(url: string) {
  return getPullRequestComments(url)
    .then(comments =>
      comments._embedded.pullRequestComments.filter(
        comment => !!comment.location
      )
    )
    .then(comments => {
      const lineComments = {};
      const files = {};

      comments.forEach(comment => {
        if (comment.location.hunk && comment.location.changeId) {
          addLineComments(lineComments, comment);
        } else {
          addFileComments(files, comment);
        }
      });

      return {
        lineComments,
        files
      };
    });
}
