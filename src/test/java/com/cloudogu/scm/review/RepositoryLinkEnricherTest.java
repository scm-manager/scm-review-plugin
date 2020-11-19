/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review;

import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.workflow.GlobalEngineConfigurator;
import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  PullRequestService pullRequestService;
  @Mock
  private HalAppender appender;
  @Mock
  private ConfigService configService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private GlobalEngineConfigurator globalEngineConfigurator;
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
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldEnrichRepositoriesWithBranchSupport() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(false);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequest", "https://scm-manager.org/scm/api/v2/pull-requests/space/name");
    verify(appender).appendLink("pullRequestConfig", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/config");
  }

  @Test
  @SubjectAware(username = "dent", password = "secret")
  public void shouldNotEnrichRepositoriesForConfigWhenRepositoryConfigIsDisabled() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(true);
    mockGlobalConfig(true);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
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
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);

    when(pullRequestService.supportsPullRequests(any())).thenReturn(false);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender, never()).appendLink(any(), any());
  }

  @Test
  @SubjectAware(username = "trillian", password = "secret")
  public void shouldNotEnrichBecauseOfMissingPermission() {
    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);
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

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);
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

    enricher = new RepositoryLinkEnricher(scmPathInfoStoreProvider, pullRequestService, configService, globalEngineConfigurator);
    Repository repo = new Repository("id", "type", "space", "name");
    HalEnricherContext context = HalEnricherContext.of(repo);
    enricher.enrich(context, appender);
    verify(appender).appendLink("pullRequestCheck", "https://scm-manager.org/scm/api/v2/pull-requests/space/name/check");
  }

}
