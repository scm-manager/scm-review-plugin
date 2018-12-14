package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfo;


/**
 * This class provide resource Links
 *
 *
 * suppress squid:S1192 (String literals should not be duplicated)
 * because the strings are here method names and it is easier to read that:
 * method("getPullRequestResource").parameters().method("get") instead of that:
 * method(GET_PULL_REQUEST_RESOURCE).parameters().method(GET)
 *
 * @author Mohamed Karray
 */
@SuppressWarnings("squid:S1192")
public class PullRequestResourceLinks {

  private ScmPathInfo scmPathInfo;

  public PullRequestResourceLinks(ScmPathInfo scmPathInfo) {
    this.scmPathInfo = scmPathInfo;
  }

  public PullRequestLinks pullRequest() {
    return new PullRequestLinks(scmPathInfo);
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

    public String reject(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("reject").parameters(namespace, name, pullRequestId)
        .href();
    }
  }

  public PullRequestCommentsLinks pullRequestComments() {
    return new PullRequestCommentsLinks(scmPathInfo);
  }

  public static class PullRequestCommentsLinks {
    private final LinkBuilder linkBuilder;

    PullRequestCommentsLinks(ScmPathInfo pathInfo) {
      linkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class);
    }

    public String all(String namespace, String name, String pullRequestId) {
      return linkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("getAll").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String create(String namespace, String name, String pullRequestId) {
      return linkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("create").parameters(namespace, name, pullRequestId)
        .href();
    }
  }

}
