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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.config.service.RepositoryPullRequestConfig;
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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
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
