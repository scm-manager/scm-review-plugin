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

import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import sonia.scm.store.InMemoryByteConfigurationStore;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BranchRestrictionsUpdateStepTest {

  private final InMemoryByteConfigurationStoreFactory storeFactory = new InMemoryByteConfigurationStoreFactory();

  @Test
  void shouldUpdateBranchBypass() throws IOException {
    InMemoryByteConfigurationStore<Object> store = (InMemoryByteConfigurationStore<Object>) storeFactory.withType(Object.class)
      .withName("pullRequestConfig")
      .build();
    store
      .setRawXml(Resources.toString(Resources.getResource("com/cloudogu/scm/review/config/update/old-pull-request-config.xml"), StandardCharsets.UTF_8));

    new BranchRestrictionsUpdateStep(storeFactory)
      .doUpdate();

    GlobalPullRequestConfig pullRequestConfig = storeFactory.withType(GlobalPullRequestConfig.class)
      .withName("pullRequestConfig")
      .build()
      .get();

    assertThat(pullRequestConfig.getProtectedBranchPatterns()).hasSize(2);
    assertThat(pullRequestConfig.getProtectedBranchPatterns().get(0))
      .hasFieldOrPropertyWithValue("branch", "main")
      .hasFieldOrPropertyWithValue("path", "*");
    assertThat(pullRequestConfig.getProtectedBranchPatterns().get(1))
      .hasFieldOrPropertyWithValue("branch", "master")
      .hasFieldOrPropertyWithValue("path", "*");
  }
}
