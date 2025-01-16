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

import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.workflow.EngineConfigService;
import com.cloudogu.scm.review.workflow.EngineConfiguration;
import com.cloudogu.scm.review.workflow.GlobalEngineConfiguration;
import com.cloudogu.scm.review.workflow.GlobalEngineConfigurator;
import com.cloudogu.scm.review.workflow.RepositoryEngineConfigurator;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
  PullRequestService pullRequestService;
  @Mock
  private HalAppender appender;
  @Mock
  private ConfigService configService;

  @Mock
  private RepositoryEngineConfigurator repositoryEngineConfigurator;

  @Mock
  private GlobalEngineConfigurator globalEngineConfigurator;
  @InjectMocks
  private EngineConfigService engineConfigService;
  private RepositoryLinkEnricher enricher;

  public RepositoryLinkEnricherTest() {
    // cleanup state that might have been left by other tests
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    ThreadContext.remove();
  }

  @Before
  public void setUp() {
    ScmPathInfoStore scmPathInfoStore = new ScmPathInfoStore();
    scmPathInfoStore.set(() -> URI.create("https://scm-manager.org/scm/api/"));
    scmPathInfoStoreProvider = Providers.of(scmPathInfoStore);

    when(repositoryEngineConfigurator.getEngineConfiguration(any())).thenReturn(new EngineConfiguration());
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichRepositoriesWithBranchSupport() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(false);
    when(configService.getNamespacePullRequestConfig(any())).thenReturn(new NamespacePullRequestConfig());
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher.enrich(context, appender);

    verify(appender).appendLink("pullRequest", "https://scm-manager.org/scm/api/v2/pull-requests/space/name");
    verify(appender).appendLink("pullRequestConfig", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/config");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotEnrichRepositoriesForConfigWhenRepositoryConfigIsDisabled() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(true);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setDisableRepositoryConfiguration(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);

    enricher.enrich(context, appender);

    verify(appender).appendLink("pullRequest", "https://scm-manager.org/scm/api/v2/pull-requests/space/name");
    verify(appender, never()).appendLink(eq("pullRequestConfig"), any());
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotEnrichRepositoriesForConfigWhenRepositoryConfigForNamespaceIsDisabled() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(false);
    NamespacePullRequestConfig namespacePullRequestConfig = new NamespacePullRequestConfig();
    namespacePullRequestConfig.setDisableRepositoryConfiguration(true);
    when(configService.getNamespacePullRequestConfig(any())).thenReturn(namespacePullRequestConfig);

    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setDisableRepositoryConfiguration(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);

    enricher.enrich(context, appender);

    verify(appender).appendLink("pullRequest", "https://scm-manager.org/scm/api/v2/pull-requests/space/name");
    verify(appender, never()).appendLink(eq("pullRequestConfig"), any());
  }


  private void mockGlobalConfig(boolean disableRepoConfig) {
    GlobalPullRequestConfig globalConfig = new GlobalPullRequestConfig();
    globalConfig.setDisableRepositoryConfiguration(disableRepoConfig);
    when(configService.getGlobalPullRequestConfig()).thenReturn(globalConfig);
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotEnrichRepositoriesWithoutBranchSupport() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(false);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotEnrichBecauseOfMissingPermission() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichWorkflowConfigLink() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("workflowConfig", "https://scm-manager.org/scm/api/v2/workflow/space/name/config");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichPullRequestCheckLink() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequestCheck", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/check");
  }

  @Test
  @SubjectAware(username = "workflowReadUser", password = "secret")
  public void shouldNotBreakWithOnlyWorkflowConfigReadPermission() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());
    mockGlobalConfig(true);

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);

    verify(appender).appendLink(eq("workflowConfig"), anyString());
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichPullRequestTemplateLink() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(false);
    when(configService.getNamespacePullRequestConfig(any())).thenReturn(new NamespacePullRequestConfig());
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequestTemplate", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/template");
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotEnrichPullRequestTemplateLinkIfUserNotPermitted() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldEnrichPullRequestSuggestionLinkIfPullRequestsAreSupported() {
    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, engineConfigService);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequestSuggestions", "https://scm-manager.org/scm/api/v2/push-entries/space/name");
  }
}
