import { getPullRequestComments } from "../pullRequest";
import { createChangeIdFromLocation, createHunkIdFromLocation, isInlineLocation } from "./locations";
import { Comment, Location } from "../types/PullRequest";
import { Link } from "@scm-manager/ui-types";

export type FileCommentCollection = {
  // path
  [key: string]: {
    comments: Comment[];
  };
};
export type LineCommentCollection = {
  // hunkid
  [key: string]: {
    // changeid
    [key: string]: {
      location: Location;
      comments: Comment[];
    };
  };
};
export type DiffRelatedCommentCollection = {
  files: FileCommentCollection;
  lines: LineCommentCollection;
  createLink: Link;
};

function addFileComments(fileComments: FileCommentCollection, comment: Comment) {
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

function addLineComments(lineComments: LineCommentCollection, comment: Comment) {
  const location = comment.location;
  if (!location) {
    // should never happen, mostly to make flow happy
    throw new Error("location is not defined");
  }

  const hunkId = createHunkIdFromLocation(location);
  const changeId = createChangeIdFromLocation(location);
  const hunkComments = lineComments[hunkId] || {};

  const changeComments = hunkComments[changeId] || {
    location,
    comments: []
  };
  changeComments.comments.push(comment);
  hunkComments[changeId] = changeComments;

  lineComments[hunkId] = hunkComments;
}

export function fetchDiffRelatedComments(url: string): DiffRelatedCommentCollection {
  return getPullRequestComments(url)
    .then(comments => {
      return {
        comments: comments._embedded.pullRequestComments.filter(comment => !!comment.location),
        createLink: comments._links.create
      };
    })
    .then(comments => {
      const lines = {};
      const files = {};

      comments.comments.forEach(comment => {
        if (isInlineLocation(comment.location) && !comment.outdated) {
          addLineComments(lines, comment);
        } else {
          addFileComments(files, comment);
        }
      });

      return {
        lines,
        files,
        createLink: comments.createLink
      };
    });
}
