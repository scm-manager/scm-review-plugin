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

import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.migration.NamespaceUpdateContext;
import sonia.scm.store.InMemoryByteConfigurationStore;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchRestrictionsNamespaceUpdateStepTest {

  private final InMemoryByteConfigurationStoreFactory storeFactory = new InMemoryByteConfigurationStoreFactory();

  @Mock
  private NamespaceUpdateContext namespaceUpdateContext;

  @Test
  void shouldUpdateBranchBypass() throws IOException {
    InMemoryByteConfigurationStore<Object> store = (InMemoryByteConfigurationStore<Object>) storeFactory.withType(Object.class)
      .withName("pullRequestConfig")
      .forNamespace("hitchhiker")
      .build();
    store
      .setRawXml(Resources.toString(Resources.getResource("com/cloudogu/scm/review/config/update/old-namespace-pull-request-config.xml"), StandardCharsets.UTF_8));

    when(namespaceUpdateContext.getNamespace()).thenReturn("hitchhiker");

    new BranchRestrictionsNamespaceUpdateStep(storeFactory)
      .doUpdate(namespaceUpdateContext);

    NamespacePullRequestConfig namespacePullRequestConfig = storeFactory.withType(NamespacePullRequestConfig.class)
      .withName("pullRequestConfig")
      .forNamespace("hitchhiker")
      .build()
      .get();

    assertThat(namespacePullRequestConfig.getProtectedBranchPatterns()).hasSize(2);
    assertThat(namespacePullRequestConfig.getProtectedBranchPatterns().get(0))
      .hasFieldOrPropertyWithValue("branch", "main")
      .hasFieldOrPropertyWithValue("path", "*");
    assertThat(namespacePullRequestConfig.getProtectedBranchPatterns().get(1))
      .hasFieldOrPropertyWithValue("branch", "master")
      .hasFieldOrPropertyWithValue("path", "*");
  }
}
