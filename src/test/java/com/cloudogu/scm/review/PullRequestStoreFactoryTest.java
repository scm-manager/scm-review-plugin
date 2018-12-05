package com.cloudogu.scm.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestStoreFactoryTest {

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private DataStoreFactory dataStoreFactory;

  @Mock
  private DataStore<PullRequest> store;

  @InjectMocks
  private PullRequestStoreFactory storeFactory;

  private NamespaceAndName namespaceAndName = new NamespaceAndName("hitchhiker", "heartOfGold");

  @Test
  public void testCreate() {
    Repository repository = RepositoryTestData.createHeartOfGold("git");
    repository.setId("42");

    when(dataStoreFactory.getStore(any())).thenReturn((DataStore) store);
    when(dataStoreFactory.withType(any())).thenCallRealMethod();

    PullRequestStore store = storeFactory.create(repository);
    assertThat(store).isNotNull();
  }
}
