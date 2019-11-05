package com.cloudogu.scm.review;

import com.google.inject.util.Providers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.MergeCommandBuilder;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestLinkEnricherTest {

  private static final Repository REPOSITORY = RepositoryTestData.createHeartOfGold();
  private static final NamespaceAndName NAMESPACE_AND_NAME = REPOSITORY.getNamespaceAndName();

  @Mock
  RepositoryServiceFactory serviceFactory;
  @Mock
  RepositoryService service;
  @Mock
  MergeCommandBuilder mergeCommandBuilder;
  @Mock
  HalEnricherContext context;
  @Mock
  HalAppender appender;
  @Mock
  HalAppender.LinkArrayBuilder linkArrayBuilder;

  PullRequestLinkEnricher enricher;

  @BeforeEach
  void initMocks() {
    when(appender.linkArrayBuilder(any())).thenReturn(linkArrayBuilder);
    lenient().when(linkArrayBuilder.append(any(), any())).thenReturn(linkArrayBuilder);
    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create("/"));
    enricher = new PullRequestLinkEnricher(Providers.of(pathInfoStore), serviceFactory);

    doReturn(NAMESPACE_AND_NAME).when(context).oneRequireByType(NamespaceAndName.class);
    when(serviceFactory.create(NAMESPACE_AND_NAME)).thenReturn(service);
    when(service.getMergeCommand()).thenReturn(mergeCommandBuilder);
  }

  @Test
  void shouldEnrichSquashLink() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(true);
    enricher.enrich(context, appender);
    verify(appender).linkArrayBuilder("merge");
    verify(linkArrayBuilder).append("squash", "/v2/merge?strategy=squash");
  }


  @Test
  void shouldNotEnrichLinksIfNoStrategyAvailable() {
    when(mergeCommandBuilder.isSupported(MergeStrategy.SQUASH)).thenReturn(false);
    enricher.enrich(context, appender);
    verify(appender).linkArrayBuilder("merge");
    verify(linkArrayBuilder, never()).append("squash", "/v2/merge?strategy=squash");
  }
}
