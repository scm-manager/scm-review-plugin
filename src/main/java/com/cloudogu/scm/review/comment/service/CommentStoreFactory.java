package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequestStoreFactory;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.security.KeyGenerator;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;

public class CommentStoreFactory {

  private static final String PULL_REQUEST_COMMENT_STORE_NAME = "pullRequestComment";

  private final DataStoreFactory dataStoreFactory;

  private final KeyGenerator keyGenerator;
  private final PullRequestStoreFactory pullRequestStoreFactory;
  private final ScmEventBus eventBus;

  @Inject
  public CommentStoreFactory(DataStoreFactory dataStoreFactory, KeyGenerator keyGenerator, PullRequestStoreFactory pullRequestStoreFactory, ScmEventBus eventBus) {
    this.dataStoreFactory = dataStoreFactory;
    this.keyGenerator = keyGenerator;
    this.pullRequestStoreFactory = pullRequestStoreFactory;
    this.eventBus = eventBus;
  }

  public CommentStore create(Repository repository) {
    DataStore<PullRequestComments> store = dataStoreFactory.withType(PullRequestComments.class).withName(PULL_REQUEST_COMMENT_STORE_NAME).forRepository(repository).build();
    return new CommentStore(store, pullRequestStoreFactory, eventBus, keyGenerator);
  }

}
