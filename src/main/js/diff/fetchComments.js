// @flow
import {getPullRequestComments} from '../pullRequest';
import {createHunkIdFromLocation} from './locations';
import type {Comment} from '../types/PullRequest';

type FileComments = { [string]: Comment[] };

function addFileComments(fileComments: FileComments, comment: Comment) {
  // $FlowFixMe file is not undefined
  const file = comment.location.file;

  const comments = fileComments[file] || [];
  comments.push( comment );

  fileComments[file] = comments;
}

export function fetchComments(url: string) {
  return getPullRequestComments(url)
    .then(comments => comments._embedded.pullRequestComments.filter((comment) => !!comment.location))
    .then(comments => {
      const fetchedComments = {
        commentLines: {

        },
        fileComments: {

        }
      };

      comments.forEach((comment) => {

        const changeId = comment.location.changeId;

        if (changeId) {
          const commentLines = fetchedComments.commentLines;

          const hunkId = createHunkIdFromLocation(comment.location);
          const commentsByChangeId = commentLines[hunkId] || {};

          const commentsForChangeId = commentsByChangeId[changeId] || [];
          commentsForChangeId.push( comment );
          commentsByChangeId[comment.location.changeId] = commentsForChangeId;

          commentLines[hunkId] = commentsByChangeId;
        } else {
          addFileComments(fetchedComments.fileComments, comment);
        }
      });

      return fetchedComments;
    })
}
