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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.InMemoryByteConfigurationStore;
import sonia.scm.store.InMemoryByteConfigurationStoreFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExplicitOverwriteFlagInConfigurationUpdateStepTest {

  private final InMemoryByteConfigurationStoreFactory configurationStoreFactory = new InMemoryByteConfigurationStoreFactory();

  private final ExplicitOverwriteFlagInConfigurationUpdateStep updateStep = new ExplicitOverwriteFlagInConfigurationUpdateStep(configurationStoreFactory);

  private final RepositoryUpdateContext context = new RepositoryUpdateContext("42");

  @Test
  void shouldSkipNoneExistingStores() {
    updateStep.doUpdate(context);

    assertThat(getStore().get()).isNull();
  }

  @Test
  void shouldIgnoreOldStoresWithoutEnabledFeatures() {
    PullRequestConfig oldConfig = new PullRequestConfig(false, false, emptyList());
    getStore().setOldObject(oldConfig);

    updateStep.doUpdate(context);

    ConfigurationStore<RepositoryPullRequestConfig> store = getStore();
    assertThat(store.get().isOverwriteParentConfig()).isFalse();
  }

  @Test
  void shouldUpdateOldStoresIfRestrictionIsActivated() {
    PullRequestConfig oldConfig = new PullRequestConfig(true, false, emptyList());
    getStore().setOldObject(oldConfig);

    updateStep.doUpdate(context);

    ConfigurationStore<RepositoryPullRequestConfig> store = getStore();
    assertThat(store.get().isOverwriteParentConfig()).isTrue();
  }

  @Test
  void shouldUpdateOldStoresIfPreventMergeFromAuthorIsActivated() {
    PullRequestConfig oldConfig = new PullRequestConfig(true, false, emptyList());
    getStore().setOldObject(oldConfig);

    updateStep.doUpdate(context);

    ConfigurationStore<RepositoryPullRequestConfig> store = getStore();
    assertThat(store.get().isOverwriteParentConfig()).isTrue();
  }

  @Test
  void shouldUpdateOldStoresIfDefaultReviewersAreSet() {
    PullRequestConfig oldConfig = new PullRequestConfig(false, false, asList("arthur", "marvin"));
    getStore().setOldObject(oldConfig);

    updateStep.doUpdate(context);

    ConfigurationStore<RepositoryPullRequestConfig> store = getStore();
    assertThat(store.get().isOverwriteParentConfig()).isTrue();
  }

  private InMemoryByteConfigurationStore<RepositoryPullRequestConfig> getStore() {
    return (InMemoryByteConfigurationStore<RepositoryPullRequestConfig>) configurationStoreFactory
      .withType(RepositoryPullRequestConfig.class)
      .withName("pullRequestConfig")
      .forRepository("42")
      .build();
  }
}

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "config")
@Getter
@Setter
@NoArgsConstructor
class PullRequestConfig {

  private boolean restrictBranchWriteAccess = false;
  @XmlElement(name = "protected-branch-patterns")
  private List<String> protectedBranchPatterns = new ArrayList<>();
  @XmlElement(name = "protection-bypasses")
  private List<ProtectionBypass> branchProtectionBypasses = new ArrayList<>();
  private boolean preventMergeFromAuthor = false;
  private List<String> defaultReviewers = new ArrayList<>();

  public PullRequestConfig(boolean restrictBranchWriteAccess, boolean preventMergeFromAuthor, List<String> defaultReviewers) {
    this.restrictBranchWriteAccess = restrictBranchWriteAccess;
    this.preventMergeFromAuthor = preventMergeFromAuthor;
    this.defaultReviewers = defaultReviewers;
  }

  @Getter
  @Setter
  @XmlRootElement(name = "bypass")
  @XmlAccessorType(XmlAccessType.FIELD)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProtectionBypass {
    private String name;
    private boolean group;
  }
}
