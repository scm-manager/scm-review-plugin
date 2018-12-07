package com.cloudogu.scm.review.comment.service;

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import javax.inject.Inject;

public class CommentStoreFactory {

  private static final String PULL_REQUEST_COMMENT_STORE_DIRECTORY = "pullRequestComment";

  private final DataStoreFactory dataStoreFactory;

  @Inject
  public CommentStoreFactory(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  public CommentStore create(Repository repository) {
    DataStore<PullRequestComments> store = dataStoreFactory.withType(PullRequestComments.class).withName(PULL_REQUEST_COMMENT_STORE_DIRECTORY).forRepository(repository).build();
    return new CommentStore(store);
  }

}
