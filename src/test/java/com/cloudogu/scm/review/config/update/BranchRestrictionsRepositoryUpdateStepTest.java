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

package com.cloudogu.scm.review.config.update;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.store.InMemoryByteConfigurationStore;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchRestrictionsRepositoryUpdateStepTest {

  private final InMemoryByteConfigurationStoreFactory storeFactory = new InMemoryByteConfigurationStoreFactory();

  @Mock
  private RepositoryUpdateContext repositoryUpdateContext;

  @Test
  void shouldUpdateBranchBypass() throws IOException {
    InMemoryByteConfigurationStore<Object> store = (InMemoryByteConfigurationStore<Object>) storeFactory.withType(Object.class)
      .withName("pullRequestConfig")
      .forRepository("42")
      .build();
    store
      .setRawXml(Resources.toString(Resources.getResource("com/cloudogu/scm/review/config/update/old-repository-pull-request-config.xml"), StandardCharsets.UTF_8));

    when(repositoryUpdateContext.getRepositoryId()).thenReturn("42");

    new BranchRestrictionsRepositoryUpdateStep(storeFactory)
      .doUpdate(repositoryUpdateContext);

    RepositoryPullRequestConfig repositoryPullRequestConfig = storeFactory.withType(RepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository("42")
      .build()
      .get();

    assertThat(repositoryPullRequestConfig.getProtectedBranchPatterns()).hasSize(1);
    assertThat(repositoryPullRequestConfig.getProtectedBranchPatterns().get(0))
      .hasFieldOrPropertyWithValue("branch", "feature/*")
      .hasFieldOrPropertyWithValue("path", "*");
  }
}
