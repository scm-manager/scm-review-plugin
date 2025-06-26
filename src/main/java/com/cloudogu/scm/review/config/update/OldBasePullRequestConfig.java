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

import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sonia.scm.repository.api.MergeStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
class OldBasePullRequestConfig {
  private MergeStrategy defaultMergeStrategy = MergeStrategy.MERGE_COMMIT;
  private boolean deleteBranchOnMerge = false;
  private boolean restrictBranchWriteAccess = false;
  @XmlElement(name = "protected-branch-patterns")
  private List<String> protectedBranchPatterns = new ArrayList<>();
  private List<String> defaultTasks = new ArrayList<>();
  @XmlElement(name = "protection-bypasses")
  private List<BasePullRequestConfig.ProtectionBypass> branchProtectionBypasses = new ArrayList<>();
  private boolean preventMergeFromAuthor = false;
  private List<String> defaultReviewers = new ArrayList<>();
  private Set<String> labels = new HashSet<>();
  private boolean overwriteDefaultCommitMessage;
  private String commitMessageTemplate;

  @Getter
  @Setter
  @XmlRootElement(name = "branchProtection")
  @XmlAccessorType(XmlAccessType.FIELD)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BranchProtection {
    private String branch;
    private String path;
  }

  void fill(BasePullRequestConfig config) {
    config.setDefaultMergeStrategy(defaultMergeStrategy);
    config.setDeleteBranchOnMerge(deleteBranchOnMerge);
    config.setProtectedBranchPatterns(protectedBranchPatterns.stream().map(branch -> new BasePullRequestConfig.BranchProtection(branch, "*")).collect(Collectors.toList()));
    config.setRestrictBranchWriteAccess(restrictBranchWriteAccess);
    config.setDefaultTasks(defaultTasks);
    config.setBranchProtectionBypasses(branchProtectionBypasses);
    config.setPreventMergeFromAuthor(preventMergeFromAuthor);
    config.setDefaultReviewers(defaultReviewers);
    config.setLabels(labels);
    config.setOverwriteDefaultCommitMessage(overwriteDefaultCommitMessage);
    config.setCommitMessageTemplate(commitMessageTemplate);
  }
}
