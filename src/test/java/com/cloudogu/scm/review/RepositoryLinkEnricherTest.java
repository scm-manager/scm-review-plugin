package com.cloudogu.scm.review;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SubjectAware(configuration = "classpath:com/cloudogu/scm/review/shiro.ini")
public class RepositoryLinkEnricherTest {

  private Provider<ScmPathInfoStore> scmPathInfoStoreProvider;

  @Rule
  public ShiroRule shiro = new ShiroRule();

  @Mock
  RepositoryServiceFactory serviceFactory;
  @Mock
  private HalAppender appender;
  private RepositoryLinkEnricher enricher;
  private RepositoryService service;

  public RepositoryLinkEnricherTest() {
    // cleanup state that might have been left by other tests
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    ThreadContext.remove();
  }

  @Before
  public void setUp() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    service = mock(RepositoryService.class);
    when(serviceFactory.create(any(Repository.class))).thenReturn(service);
    scmPathInfoStore.set(() -> URI.create("https://scm-manager.org/scm/api/"));
    scmPathInfoStoreProvider = Providers.of(scmPathInfoStore);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichRepositoriesWithBranchSupport() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, serviceFactory);

    when(service.isSupported(Command.MERGE)).thenReturn(true);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequest", "https://scm-manager.org/scm/api/v2/pull-requests/space/name");
    verify(appender).appendLink("pullRequestConfig", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/config");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotEnrichRepositoriesWithoutBranchSupport() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, serviceFactory);

    when(service.isSupported(Command.MERGE)).thenReturn(false);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotEnrichBecauseOfMissingPermission() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, serviceFactory);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(), any());
  }
}
