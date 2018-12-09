package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfo;
import sonia.scm.api.v2.resources.ScmPathInfoStore;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PullRequestResourceLinks {

  private final ScmPathInfoStore scmPathInfoStore;

  @Inject
  public PullRequestResourceLinks(ScmPathInfoStore scmPathInfoStore) {
    this.scmPathInfoStore = scmPathInfoStore;
  }

  public PullRequestLinks pullRequest() {
    return new PullRequestLinks(scmPathInfoStore.get());
  }

  public static class PullRequestLinks {
    private final LinkBuilder pullRequestLinkBuilder;

    PullRequestLinks(ScmPathInfo pathInfo) {
      pullRequestLinkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class, PullRequestResource.class);
    }

    public String self(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("get").parameters(namespace, name, pullRequestId)
        .href();
    }
  }

  public PullRequestCommentLinks pullRequestComment() {
    return new PullRequestCommentLinks(scmPathInfoStore.get());
  }

  public static class PullRequestCommentLinks {
    private final LinkBuilder pullRequestCommentLinkBuilder;

    PullRequestCommentLinks(ScmPathInfo pathInfo) {
      pullRequestCommentLinkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class);
    }

    public String all(String namespace, String name, String pullRequestId) {
      return pullRequestCommentLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("getAll").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String create(String namespace, String name, String pullRequestId) {
      return pullRequestCommentLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("create").parameters(namespace, name, pullRequestId)
        .href();
    }
  }

}
