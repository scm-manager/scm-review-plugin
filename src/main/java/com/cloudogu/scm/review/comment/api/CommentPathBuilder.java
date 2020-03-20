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
package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import javax.inject.Inject;
import javax.inject.Provider;

import static com.cloudogu.scm.review.LinkRevisionAppender.append;

class CommentPathBuilder {

  private final Provider<ScmPathInfoStore> pathInfoStore;

  @Inject
  CommentPathBuilder(Provider<ScmPathInfoStore> pathInfoStore) {
    this.pathInfoStore = pathInfoStore;
  }

  String createCommentSelfUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("getComment").parameters()
      .href();
  }

  public String createReplySelfUri(String namespace, String name, String pullRequestId, String commentId, String replyId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("getReply").parameters(replyId)
      .href();
  }

  String createUpdateCommentUri(String namespace, String name, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateComment").parameters()
      .href();
    return append(link, revisions);
  }

  String createDeleteCommentUri(String namespace, String name, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("deleteComment").parameters()
      .href();
    return append(link, revisions);
  }

  String createReplyCommentUri(String namespace, String name, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("reply").parameters()
      .href();
    return append(link, revisions);
  }

  String createUpdateReplyUri(String namespace, String name, String pullRequestId, String commentId, String replyId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateReply").parameters(replyId)
      .href();

    return append(link, revisions);
  }

  String createDeleteReplyUri(String namespace, String name, String pullRequestId, String commentId, String replyId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("deleteReply").parameters(replyId)
      .href();

    return append(link, revisions);
  }

  String createPossibleTransitionUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("transform").parameters()
      .href();
  }

  public String createExecutedTransitionUri(String namespace, String name, String pullRequestId, String commentId, String transitionId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("getExecutedTransition").parameters(transitionId)
      .href();
  }
}
