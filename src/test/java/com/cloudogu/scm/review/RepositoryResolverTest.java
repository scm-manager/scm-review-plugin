package com.cloudogu.scm.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryResolverTest {

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private RepositoryManager repositoryManager;

  @InjectMocks
  private RepositoryResolver resolver;

  private NamespaceAndName namespaceAndName = new NamespaceAndName("hitchhiker", "heartOfGold");

  @Test
  public void testResolve() {
    Repository repository = RepositoryTestData.createHeartOfGold("git");
    repository.setId("42");

    when(repositoryManager.get(namespaceAndName)).thenReturn(repository);
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(Boolean.TRUE);

    Repository resolvedRepository = resolver.resolve(namespaceAndName);
    assertThat(resolvedRepository).isSameAs(repository);
  }

  @Test
  public void testResolveRepositoryNotFound() {
    NotFoundException exception = assertThrows(NotFoundException.class, () -> resolver.resolve(namespaceAndName));
    ContextEntry contextEntry = exception.getContext().get(0);
    assertThat(contextEntry.getType()).isEqualTo("Repository");
    assertThat(contextEntry.getId()).isEqualTo("hitchhiker/heartOfGold");
  }

  @Test
  public void testResolveForUnsupportedType() {
    Repository repository = RepositoryTestData.createHeartOfGold("svn");
    repository.setId("42");

    when(repositoryManager.get(namespaceAndName)).thenReturn(repository);
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.isSupported(Command.MERGE)).thenReturn(Boolean.FALSE);

    PullRequestNotSupportedException exception = assertThrows(PullRequestNotSupportedException.class, () -> resolver.resolve(namespaceAndName));
    ContextEntry contextEntry = exception.getContext().get(0);
    assertThat(contextEntry.getType()).isEqualTo("Repository");
    assertThat(contextEntry.getId()).isEqualTo("hitchhiker/HeartOfGold");
  }

}
