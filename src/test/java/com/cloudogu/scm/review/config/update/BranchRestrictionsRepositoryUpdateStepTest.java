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
