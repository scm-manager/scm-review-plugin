package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentStoreFactoryTest {

  @Mock
  private DataStoreFactory dataStoreFactory;

  @Mock
  private DataStore<PullRequest> store;

  @InjectMocks
  private CommentStoreFactory storeFactory;

  @Test
  public void shouldCreateCommentStore() {
    Repository repository = RepositoryTestData.createHeartOfGold("git");
    repository.setId("repo");

    when(dataStoreFactory.getStore(any())).thenReturn((DataStore) store);
    when(dataStoreFactory.withType(any())).thenCallRealMethod();

    CommentStore commentStore = storeFactory.create(repository);
    assertThat(commentStore).isNotNull();
  }
}
