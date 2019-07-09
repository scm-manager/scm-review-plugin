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

  String createUpdateCommentUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateComment").parameters()
      .href();
  }

  String createDeleteCommentUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("deleteComment").parameters()
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

  String createUpdateReplyUri(String namespace, String name, String pullRequestId, String commentId, String replyId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("updateReply").parameters(replyId)
      .href();
  }

  String createDeleteReplyUri(String namespace, String name, String pullRequestId, String commentId, String replyId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("deleteReply").parameters(replyId)
      .href();
  }

  String createTransitionUri(String namespace, String name, String pullRequestId, String commentId) {
    LinkBuilder linkBuilder = new LinkBuilder(pathInfoStore.get().get(), PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class, CommentResource.class);
    return linkBuilder
      .method("getPullRequestResource").parameters(namespace, name, pullRequestId)
      .method("comments").parameters()
      .method("getCommentResource").parameters(commentId)
      .method("transform").parameters()
      .href();
  }
}
