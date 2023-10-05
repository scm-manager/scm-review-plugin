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
import sonia.scm.migration.NamespaceUpdateContext;
import sonia.scm.migration.NamespaceUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

import javax.inject.Inject;

@Extension
public class BranchRestrictionsNamespaceUpdateStep implements NamespaceUpdateStep {

  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public BranchRestrictionsNamespaceUpdateStep(ConfigurationStoreFactory configurationStoreFactory) {
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate(NamespaceUpdateContext namespaceUpdateContext) {
    String namespace = namespaceUpdateContext.getNamespace();
    ConfigurationStore<OldNamespacePullRequestConfig> namespaceStore = configurationStoreFactory
      .withType(OldNamespacePullRequestConfig.class)
      .withName("pullRequestConfig")
      .forNamespace(namespace)
      .build();
    namespaceStore.getOptional().ifPresent(config -> {
      NamespacePullRequestConfig newNamespaceConfig = new NamespacePullRequestConfig();
      config.fill(newNamespaceConfig);
      newNamespaceConfig.setDisableRepositoryConfiguration(config.isDisableRepositoryConfiguration());
      newNamespaceConfig.setOverwriteParentConfig(config.isOverwriteParentConfig());
      ConfigurationStore<NamespacePullRequestConfig> newNamespaceStore = configurationStoreFactory
        .withType(NamespacePullRequestConfig.class)
        .withName("pullRequestConfig")
        .forNamespace(namespace)
        .build();
      newNamespaceStore.set(newNamespaceConfig);
    });
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.31.0");
  }

  @Override
  public String getAffectedDataType() {
    return "com.cloudogu.scm.review.config";
  }
}
