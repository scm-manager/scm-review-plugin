package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.api.MergeResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfo;
import sonia.scm.repository.api.MergeStrategy;


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

  public PullRequestCollectionLinks pullRequestCollection() {

    return new PullRequestCollectionLinks(scmPathInfo);
  }

  public static class PullRequestCollectionLinks {
    private final LinkBuilder pullRequestLinkBuilder;

    PullRequestCollectionLinks(ScmPathInfo pathInfo) {
      pullRequestLinkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class);
    }

    public String all(String namespace, String name) {
      return pullRequestLinkBuilder
        .method("getAll").parameters(namespace, name)
        .href();
    }

    public String create(String namespace, String name) {
      return pullRequestLinkBuilder
        .method("create").parameters(namespace, name)
        .href();
    }
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

    public String approve(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("approve").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String disapprove(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("disapprove").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String subscribe(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("subscribe").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String unsubscribe(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("unsubscribe").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String update(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("update").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String reject(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("reject").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String subscription(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("getSubscription").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String events(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("events").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String markAsReviewed(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("markAsReviewed").parameters(namespace, name, pullRequestId, "PATH")
        .href()
        .replace("PATH", "{path}");
    }

    public String markAsNotReviewed(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("markAsNotReviewed").parameters(namespace, name, pullRequestId, "PATH")
        .href()
        .replace("PATH", "{path}");
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

    public String create(String namespace, String name, String pullRequestId, BranchRevisionResolver.RevisionResult revisionResult) {
      String link = linkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("create").parameters(namespace, name, pullRequestId)
        .href();
      return
        LinkRevisionAppender.append(link, revisionResult);
    }
  }

  public MergeLinks mergeLinks() {
    return new MergeLinks(scmPathInfo);
  }

  public static class MergeLinks {
    private final LinkBuilder mergeLinkBuilder;

    MergeLinks(ScmPathInfo pathInfo) {
      this.mergeLinkBuilder = new LinkBuilder(pathInfo, MergeResource.class);
    }

    public String check(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder.method("check").parameters(namespace, name, pullRequestId).href();
    }

    public String merge(String namespace, String name, String pullRequestId, MergeStrategy strategy) {
      return mergeLinkBuilder
        .method("merge").parameters(namespace, name, pullRequestId).href() + "?strategy=" + strategy;
    }

    public String conflicts(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder.method("conflicts").parameters(namespace, name, pullRequestId).href();
    }
    public String createDefaultCommitMessage(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder
        .method("createDefaultCommitMessage").parameters(namespace, name, pullRequestId).href();
    }
  }
}
