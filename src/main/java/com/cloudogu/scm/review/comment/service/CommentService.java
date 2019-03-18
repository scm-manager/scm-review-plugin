package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static sonia.scm.ContextEntry.ContextBuilder.entity;
import static sonia.scm.NotFoundException.notFound;

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
   * @return the id of the created pullRequestComment
   */
  public String add(String namespace, String name, String pullRequestId, PullRequestComment pullRequestComment) {
    return getCommentStore(namespace, name).add(pullRequestId, pullRequestComment);
  }

  public String add(Repository repository, String pullRequestId, PullRequestComment pullRequestComment) {
    return getCommentStore(repository).add(pullRequestId, pullRequestComment);
  }

  private CommentStore getCommentStore(Repository repository) {
    return storeFactory.create(repository);
  }

  private CommentStore getCommentStore(String namespace, String name) {
    Repository repository = repositoryResolver.resolve(new NamespaceAndName(namespace, name));
    return getCommentStore(repository);
  }

  public List<PullRequestComment> getAll(String namespace, String name, String pullRequestId) {
    try {
      PullRequestComments value = getCommentStore(namespace, name).get(pullRequestId);
      return Optional.ofNullable(value)
        .map(PullRequestComments::getComments)
        .orElse(Lists.newArrayList());
    } catch (NotFoundException e) {
      return Lists.newArrayList();
    }
  }

  public PullRequestComment get(String namespace, String name, String pullRequestId, String commentId) {
    return Optional.ofNullable(getCommentStore(namespace, name).get(pullRequestId))
      .map(PullRequestComments::getComments)
      .orElse(Lists.newArrayList())
      .stream()
      .filter(c -> c.getId().equals(commentId))
      .findFirst()
      .orElseThrow(() -> notFound(entity(PullRequestComment.class, String.valueOf(commentId))
        .in(PullRequest.class, pullRequestId)
        .in(new NamespaceAndName(namespace, name))));
  }


  public void delete(String namespace, String name, String pullRequestId, String commentId) {
    getCommentStore(namespace, name).delete(pullRequestId, commentId);
  }

  public void update(String namespace, String name, String pullRequestId, String commentId, String newComment) {
    getCommentStore(namespace, name).update(pullRequestId, commentId, newComment);
  }

  /**
   * Add a system comment about the status Change of the pull request
   *
   * @param repository the repository
   * @param pullRequestId the pull request id
   */
  public void addStatusChangedComment(Repository repository, String pullRequestId, SystemCommentType commentType) {
    String user = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
    PullRequestComment comment = new PullRequestComment();
    comment.setDate(Instant.now());
    comment.setAuthor(user);
    comment.setSystemComment(true);
    comment.setComment(commentType.getKey());
    add(repository ,pullRequestId,comment);
  }

}
