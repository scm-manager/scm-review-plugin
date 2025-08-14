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

package com.cloudogu.scm.review;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.api.MergeResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.dto.BranchRevisionResolver;
import com.cloudogu.scm.review.workflow.EngineResultResource;
import com.cloudogu.scm.review.workflow.GlobalEngineConfigResource;
import com.cloudogu.scm.review.workflow.RepositoryEngineConfigResource;
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

    public String check(String namespace, String name) {
      return pullRequestLinkBuilder
        .method("check").parameters(namespace, name)
        .href();
    }

    public String template(String namespace, String name) {
      return pullRequestLinkBuilder
        .method("getPullRequestTemplate").parameters(namespace, name)
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

    public String rejectWithMessage(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("rejectWithMessage").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String reopen(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("reopen").parameters(namespace, name, pullRequestId)
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

    public String reviewMark(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("markAsReviewed").parameters(namespace, name, pullRequestId, "PATH")
        .href()
        .replace("PATH", "{path}");
    }

    public String convertToPR(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("convertToPR").parameters(namespace, name, pullRequestId)
        .href();
    }

    public String check(String namespace, String name, String pullRequestId) {
      return pullRequestLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("check").parameters(namespace, name, pullRequestId)
        .href();
    }
  }

  public PullRequestChangesLinks pullRequestChanges() {
    return new PullRequestChangesLinks(scmPathInfo);
  }

  public static class PullRequestChangesLinks {
    private final LinkBuilder linkBuilder;

    public PullRequestChangesLinks(ScmPathInfo pathInfo) {
      linkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class, PullRequestResource.class);
    }

    public String readAll(String namespace, String name, String pullRequestId) {
      return linkBuilder
        .method("getPullRequestResource").parameters()
        .method("getChanges").parameters(namespace, name, pullRequestId)
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

    public String create(String namespace, String name, String pullRequestId, BranchRevisionResolver.RevisionResult revisionResult) {
      String link = linkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("create").parameters(namespace, name, pullRequestId)
        .href();
      return
        LinkRevisionAppender.append(link, revisionResult);
    }

    public String createWithImages(String namespace, String name, String pullRequestId, BranchRevisionResolver.RevisionResult revisionResult) {
      String link = linkBuilder
        .method("getPullRequestResource").parameters()
        .method("comments").parameters()
        .method("createWithImage").parameters(namespace, name, pullRequestId)
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

    public String emergencyMerge(String namespace, String name, String pullRequestId, MergeStrategy strategy) {
      return mergeLinkBuilder
        .method("emergencyMerge").parameters(namespace, name, pullRequestId).href() + "?strategy=" + strategy;
    }

    public String conflicts(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder.method("conflicts").parameters(namespace, name, pullRequestId).href();
    }

    public String createDefaultCommitMessage(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder
        .method("createDefaultCommitMessage").parameters(namespace, name, pullRequestId).href();
    }

    public String getMergeStrategyInfo(String namespace, String name, String pullRequestId) {
      return mergeLinkBuilder
        .method("getMergeStrategyInfo").parameters(namespace, name, pullRequestId).href();
    }
  }

  public WorkflowEngineLinks workflowEngineLinks() {
    return new WorkflowEngineLinks(scmPathInfo);
  }

  public static class WorkflowEngineLinks {
    private final LinkBuilder workflowEngineLinkBuilder;

    public WorkflowEngineLinks(ScmPathInfo pathInfo) {
      this.workflowEngineLinkBuilder = new LinkBuilder(pathInfo, PullRequestRootResource.class, PullRequestResource.class, EngineResultResource.class);
    }

    public String results(String namespace, String name, String pullRequestId) {
      return workflowEngineLinkBuilder
        .method("getPullRequestResource").parameters()
        .method("workflowResults").parameters()
        .method("getResult").parameters(namespace, name, pullRequestId).href();
    }
  }

  public WorkflowEngineConfigLinks workflowEngineConfigLinks() {
    return new WorkflowEngineConfigLinks(scmPathInfo);
  }

  public static class WorkflowEngineConfigLinks {
    private final LinkBuilder workflowEngineConfigLinkBuilder;

    public WorkflowEngineConfigLinks(ScmPathInfo pathInfo) {
      this.workflowEngineConfigLinkBuilder = new LinkBuilder(pathInfo, RepositoryEngineConfigResource.class);
    }

    public String getConfig(String namespace, String name) {
      return workflowEngineConfigLinkBuilder
        .method("getRepositoryEngineConfig").parameters(namespace, name).href();
    }

    public String setConfig(String namespace, String name) {
      return workflowEngineConfigLinkBuilder
        .method("setRepositoryEngineConfig").parameters(namespace, name).href();
    }
  }

  public WorkflowEngineGlobalConfigLinks workflowEngineGlobalConfigLinks() {
    return new WorkflowEngineGlobalConfigLinks(scmPathInfo);
  }

  public static class WorkflowEngineGlobalConfigLinks {
    private final LinkBuilder workflowEngineGlobalConfigLinkBuilder;

    public WorkflowEngineGlobalConfigLinks(ScmPathInfo pathInfo) {
      this.workflowEngineGlobalConfigLinkBuilder = new LinkBuilder(pathInfo, GlobalEngineConfigResource.class);
    }

    public String getConfig() {
      return workflowEngineGlobalConfigLinkBuilder
        .method("getGlobalEngineConfig").parameters().href();
    }

    public String setConfig() {
      return workflowEngineGlobalConfigLinkBuilder
        .method("setGlobalEngineConfig").parameters().href();
    }

    public String availableRules() {
      return workflowEngineGlobalConfigLinkBuilder
        .method("getAvailableRules").parameters().href();
    }
  }
}
