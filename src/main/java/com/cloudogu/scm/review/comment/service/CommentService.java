package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.user.User;

import javax.inject.Inject;
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
   * A User can modify a comment if he is the author or he has a push permission
   *
   * @param pullRequestId
   * @param commentId
   * @param repository
   * @return true if the user can update/delete a comment
   */
  public boolean modificationsAllowed(String pullRequestId, String commentId, Repository repository) {
    return modificationsAllowed(repository, this.get(repository.getNamespace(), repository.getName(), pullRequestId, commentId));
  }

  public boolean modificationsAllowed(Repository repository, PullRequestComment requestComment ) {
    String currentUser = SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();

    return currentUser.equals(requestComment.getAuthor())
      || RepositoryPermissions.push(repository).isPermitted();
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

  private CommentStore getCommentStore(String namespace, String name) {
    return storeFactory.create(repositoryResolver.resolve(new NamespaceAndName(namespace, name)));
  }

}
