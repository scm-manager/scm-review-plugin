package com.cloudogu.scm.review.config.update;

import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.GlobalPullRequestConfig;
import com.cloudogu.scm.review.config.service.NamespacePullRequestConfig;
import sonia.scm.migration.UpdateStep;
import sonia.scm.repository.NamespaceManager;
import sonia.scm.store.ConfigurationStore;
import sonia.scm.store.ConfigurationStoreFactory;
import sonia.scm.version.Version;

import javax.inject.Inject;
import java.util.stream.Collectors;

public class BranchRestrictionsUpdateStep implements UpdateStep {

  private final NamespaceManager namespaceManager;
  private final ConfigurationStoreFactory configurationStoreFactory;

  @Inject
  public BranchRestrictionsUpdateStep(NamespaceManager namespaceManager, ConfigurationStoreFactory configurationStoreFactory) {
    this.namespaceManager = namespaceManager;
    this.configurationStoreFactory = configurationStoreFactory;
  }

  @Override
  public void doUpdate() {
    ConfigurationStore<OldGlobalPullRequestConfig> store = configurationStoreFactory
      .withType(OldGlobalPullRequestConfig.class)
      .withName("pullRequestConfig")
      .build();
    ConfigurationStore<GlobalPullRequestConfig> newStore = configurationStoreFactory
      .withType(GlobalPullRequestConfig.class)
      .withName("pullRequestConfig")
      .build();
    store.getOptional().ifPresent(config -> {
      GlobalPullRequestConfig newGlobalConfig = new GlobalPullRequestConfig();
      newGlobalConfig.setDisableRepositoryConfiguration(config.isDisableRepositoryConfiguration());
      newGlobalConfig.setBranchProtectionBypasses(config.getBranchProtectionBypasses());
      newGlobalConfig.setCommitMessageTemplate(config.getCommitMessageTemplate());
      newGlobalConfig.setDefaultReviewers(config.getDefaultReviewers());
      newGlobalConfig.setDefaultTasks(config.getDefaultTasks());
      newGlobalConfig.setDeleteBranchOnMerge(config.isDeleteBranchOnMerge());
      newGlobalConfig.setDefaultMergeStrategy(config.getDefaultMergeStrategy());
      newGlobalConfig.setLabels(config.getLabels());
      newGlobalConfig.setOverwriteDefaultCommitMessage(config.isOverwriteDefaultCommitMessage());
      newGlobalConfig.setPreventMergeFromAuthor(config.isPreventMergeFromAuthor());
      newGlobalConfig.setProtectedBranchPatterns(config.getProtectedBranchPatterns().stream().map(branch -> new BasePullRequestConfig.BranchProtection(branch, "*")).collect(Collectors.toList()));
      newGlobalConfig.setRestrictBranchWriteAccess(config.isRestrictBranchWriteAccess());
      newStore.set(newGlobalConfig);
    });

    namespaceManager.getAll().forEach(namespace -> {
      ConfigurationStore<OldNamespacePullRequestConfig> namespaceStore = configurationStoreFactory
        .withType(OldNamespacePullRequestConfig.class)
        .withName("pullRequestConfig")
        .forNamespace(namespace.getNamespace())
        .build();
      ConfigurationStore<NamespacePullRequestConfig> newNamespaceStore = configurationStoreFactory
        .withType(NamespacePullRequestConfig.class)
        .withName("pullRequestConfig")
        .forNamespace(namespace.getNamespace())
        .build();
      namespaceStore.getOptional().ifPresent(config -> {
        NamespacePullRequestConfig newNamespaceConfig = new NamespacePullRequestConfig();
        newNamespaceConfig.setDisableRepositoryConfiguration(config.isDisableRepositoryConfiguration());
        newNamespaceConfig.setBranchProtectionBypasses(config.getBranchProtectionBypasses());
        newNamespaceConfig.setCommitMessageTemplate(config.getCommitMessageTemplate());
        newNamespaceConfig.setDefaultReviewers(config.getDefaultReviewers());
        newNamespaceConfig.setDefaultTasks(config.getDefaultTasks());
        newNamespaceConfig.setDeleteBranchOnMerge(config.isDeleteBranchOnMerge());
        newNamespaceConfig.setDefaultMergeStrategy(config.getDefaultMergeStrategy());
        newNamespaceConfig.setLabels(config.getLabels());
        newNamespaceConfig.setOverwriteDefaultCommitMessage(config.isOverwriteDefaultCommitMessage());
        newNamespaceConfig.setPreventMergeFromAuthor(config.isPreventMergeFromAuthor());
        newNamespaceConfig.setProtectedBranchPatterns(config.getProtectedBranchPatterns().stream().map(branch -> new BasePullRequestConfig.BranchProtection(branch, "*")).collect(Collectors.toList()));
        newNamespaceConfig.setRestrictBranchWriteAccess(config.isRestrictBranchWriteAccess());
        newNamespaceConfig.setOverwriteParentConfig(config.isOverwriteParentConfig());
        newNamespaceStore.set(newNamespaceConfig);
      });
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
