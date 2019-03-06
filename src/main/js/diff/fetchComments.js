// @flow
import {getPullRequestComments} from '../pullRequest';
import {createHunkIdFromLocation} from './locations';

export function fetchComments(url: string) {
  return getPullRequestComments(url)
    .then(comments => comments._embedded.pullRequestComments.filter((comment) => !!comment.location))
    .then(comments => {
      const fetchedComments = {
        commentLines: {

        },
        commentFile: {

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
          const commentFiles = fetchedComments.commentFile;

          const fileComments = commentFiles[comment.location.file] || [];
          fileComments.push( comment );

          commentFiles[comment.location.file] = fileComments;
        }
      });

      return fetchedComments;
    })
}
