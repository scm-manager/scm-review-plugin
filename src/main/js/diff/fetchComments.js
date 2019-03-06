// @flow
import {getPullRequestComments} from '../pullRequest';
import {createHunkIdFromLocation} from './locations';

export function fetchComments(url: string) {
  return getPullRequestComments(url)
    .then(comments => comments._embedded.pullRequestComments.filter((comment) => !!comment.location))
    .then(comments => {
      const commentLines = {};

      comments.forEach((comment) => {

        const hunkId = createHunkIdFromLocation(comment.location);
        const commentsByChangeId = commentLines[hunkId] || {};

        const commentsForChangeId = commentsByChangeId[comment.location.changeId] || [];
        commentsForChangeId.push( comment );
        commentsByChangeId[comment.location.changeId] = commentsForChangeId;

        commentLines[hunkId] = commentsByChangeId;
      });
      return commentLines;
    })
}
