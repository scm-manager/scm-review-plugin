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

package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

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

  String createUpdateCommentWithImageUri(String namespace, String name, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateCommentWithImage").parameters()
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

  String createReplyCommentWithImageUri(String namespace, String name, String pullRequestId, String commentId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("replyWithImage").parameters()
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

  String createUpdateReplyWithImageUri(String namespace, String name, String pullRequestId, String commentId, String replyId, BranchRevisionResolver.RevisionResult revisions) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    String link = linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateReplyWithImage").parameters(replyId)
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
