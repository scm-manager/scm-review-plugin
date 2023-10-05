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
