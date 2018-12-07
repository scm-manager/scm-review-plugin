package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.google.common.collect.Lists;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class CommentService {

  private final RepositoryResolver repositoryResolver;
  private final CommentStoreFactory storeFactory;

  @Inject
  public CommentService(RepositoryResolver repositoryResolver, CommentStoreFactory storeFactory) {
    this.repositoryResolver = repositoryResolver;
    this.storeFactory = storeFactory;
  }

  /**
   * Add a Comment to the PullRequest with id <code>pullRequestId</code>
   *
   * @param namespace
   * @param name
   * @param pullRequestId
   * @param pullRequestComment
   * @return the id of the created pullRequestComment
   */
  public String add(String namespace, String name, String pullRequestId, PullRequestComment pullRequestComment) {
    return getCommentStore(namespace, name).add(pullRequestId, pullRequestComment);
  }

  public List<PullRequestComment> getAll(String namespace, String name, String pullRequestId) {
    return Optional.ofNullable(getCommentStore(namespace, name).get(pullRequestId))
      .map(PullRequestComments::getComments)
      .orElse(Lists.newArrayList());
  }

  private CommentStore getCommentStore(String namespace, String name) {
    return storeFactory.create(repositoryResolver.resolve(new NamespaceAndName(namespace, name)));
  }
}
