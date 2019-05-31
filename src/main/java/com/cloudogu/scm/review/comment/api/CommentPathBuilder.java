package com.cloudogu.scm.review.comment.api;

import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import javax.inject.Inject;
import javax.inject.Provider;

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
      .method("get").parameters()
      .href();
  }

  String createUpdateCommentUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("update").parameters()
      .href();
  }

  String createDeleteCommentUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("delete").parameters()
      .href();

  }

  String createReplyCommentUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("reply").parameters()
      .href();

  }
}
